package com.ssafy.prj.model.service.neighborhood;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.chat.service.RagSearchResult;
import com.ssafy.prj.chat.service.RagSearchService;
import com.ssafy.prj.model.dto.neighborhood.CategoryScore;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSummaryRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSummaryResponse;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

/**
 * 추천 점수와 RAG 문서를 함께 사용해 동네 상세 요약을 만든다.
 *
 * <p>2차 구현부터는 Spring AI {@link ChatClient}로 외부 LLM을 호출한다. 다만 LLM 호출은 네트워크,
 * API 키, 응답 형식 문제로 실패할 수 있으므로 항상 규칙 기반 fallback 요약을 먼저 만들어 둔다.
 * LLM 호출이 성공하면 더 자연스러운 문장을 사용하고, 실패하면 fallback을 그대로 반환한다.</p>
 *
 * <p>요약은 감성적인 비유보다 사용자가 바로 이해할 수 있는 생활 해석을 우선한다.
 * 예를 들어 "교통 점수가 높다"에서 멈추지 않고, 서울시 기준 상위 비율과 사용자의 우선순위를
 * 함께 풀어 "이 조건에서는 교통을 강점으로 볼 수 있다"처럼 설명한다.</p>
 */
@Service
public class NeighborhoodSummaryServiceImpl implements NeighborhoodSummaryService {
    private static final Logger log = LoggerFactory.getLogger(NeighborhoodSummaryServiceImpl.class);

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
        "SCHOOL", "학교·교육",
        "CAFE", "카페·외식",
        "CULTURE", "운동·문화",
        "SHOPPING", "장보기·생활편의",
        "PARK", "공원·산책",
        "MEDICAL", "의료",
        "PET", "반려인",
        "TRANSPORT", "교통"
    );

    private final RagSearchService ragSearchService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public NeighborhoodSummaryServiceImpl(
        RagSearchService ragSearchService,
        ChatClient.Builder chatClientBuilder,
        ObjectMapper objectMapper
    ) {
        this.ragSearchService = ragSearchService;
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                너는 서울시 동네 생활환경을 설명하는 부동산·생활 데이터 분석가다.
                추천 점수, 서울시 기준 상위 비율, RAG 문서 근거를 사용해서 한국어 요약을 작성한다.
                사용자가 이해하기 쉽게 풀어써라 또한 응답의 가장 상위에는 동네 요약을 간단히 해라.
                """)
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public NeighborhoodSummaryResponse summarize(NeighborhoodSummaryRequest request) {
        RecommendedNeighborhood neighborhood = request == null ? null : request.getNeighborhood();
        if (neighborhood == null) {
            throw new IllegalArgumentException("요약할 동네 정보가 필요합니다.");
        }

        List<CategoryScore> scores = safeScores(neighborhood).stream()
            .sorted(Comparator.comparingDouble(CategoryScore::getContribution).reversed())
            .toList();
        List<String> focusKeywords = focusKeywords(scores, request.getPriorities());
        List<RagSearchResult> ragResults = ragSearchService.search(
            buildSearchQuestion(neighborhood, focusKeywords, request),
            neighborhood.getSigunguName(),
            neighborhood.getDongName(),
            focusKeywords
        );

        NeighborhoodSummaryResponse fallback = buildFallbackSummary(request, neighborhood, scores, ragResults);
        return summarizeWithLlm(request, neighborhood, scores, ragResults, fallback);
    }

    /**
     * LLM이 없어도 화면이 비지 않도록 만드는 기본 요약이다.
     * LLM 호출 실패 시 이 결과가 그대로 사용자에게 전달된다.
     */
    private NeighborhoodSummaryResponse buildFallbackSummary(
        NeighborhoodSummaryRequest request,
        RecommendedNeighborhood neighborhood,
        List<CategoryScore> scores,
        List<RagSearchResult> ragResults
    ) {
        List<String> points = new java.util.ArrayList<>();
        scores.stream().limit(3).forEach(score -> points.add(scorePoint(score)));
        addHousePoint(points, request);
        addRagPoint(points, ragResults);

        String lead = buildLead(neighborhood, scores, request);
        String caution = "이 요약은 추천 점수와 RAG 문서의 공개 데이터 근거를 바탕으로 만든 해석입니다. 실제 생활 동선, 선호 시설 위치, 매물 상태는 지도와 상세 매물에서 함께 확인해주세요.";

        return new NeighborhoodSummaryResponse(lead, points, !ragResults.isEmpty(), caution);
    }

    /**
     * 외부 LLM을 호출해 fallback보다 자연스러운 설명으로 다듬는다.
     *
     * <p>응답은 프론트가 바로 쓰기 쉬운 JSON만 요구한다. 모델이 ```json 코드블록을 붙이는 경우도
     * 흔하므로 파싱 전 코드블록을 제거한다. 실패하면 로그만 남기고 fallback을 반환한다.</p>
     */
    private NeighborhoodSummaryResponse summarizeWithLlm(
        NeighborhoodSummaryRequest request,
        RecommendedNeighborhood neighborhood,
        List<CategoryScore> scores,
        List<RagSearchResult> ragResults,
        NeighborhoodSummaryResponse fallback
    ) {
        try {
            String content = chatClient.prompt()
                .user(buildLlmPrompt(request, neighborhood, scores, ragResults, fallback))
                .call()
                .content();
            NeighborhoodSummaryResponse parsed = parseLlmResponse(content, fallback.isRagDocumentFound());
            return parsed == null ? fallback : parsed;
        } catch (Exception exception) {
            log.info("[NeighborhoodSummary] LLM summary failed. fallback used. reason={}", exception.getMessage());
            return fallback;
        }
    }

    private String buildLlmPrompt(
        NeighborhoodSummaryRequest request,
        RecommendedNeighborhood neighborhood,
        List<CategoryScore> scores,
        List<RagSearchResult> ragResults,
        NeighborhoodSummaryResponse fallback
    ) {
        StringBuilder scoreLines = new StringBuilder();
        for (CategoryScore score : scores) {
            scoreLines.append("- ")
                .append(categoryLabel(score.getCategory()))
                .append(": score=").append(formatNumber(score.getScore()))
                .append(", 서울시 기준 상위 약 ").append(topPercent(score.getScore())).append("%")
                .append(", weight=").append(formatNumber(score.getWeight()))
                .append(", contribution=").append(formatNumber(score.getContribution()))
                .append('\n');
        }

        StringBuilder ragLines = new StringBuilder();
        if (ragResults == null || ragResults.isEmpty()) {
            ragLines.append("- RAG 문서 검색 결과 없음\n");
        } else {
            ragResults.stream().limit(4).forEach(result -> ragLines.append("- [")
                .append(result.sectionTitle())
                .append("] ")
                .append(trimForPrompt(result.content(), 180))
                .append('\n'));
        }

        return """
            아래 데이터를 바탕으로 동네 추천 상세 화면의 "AI가 분석한 동네 요약"을 만들어줘.

            [작성 규칙]
            - 반드시 JSON만 출력해. 마크다운 코드블록을 붙이지 마.
            - JSON 스키마: {"lead": "문장", "points": ["문장", "문장", "문장"], "caution": "문장"}
            - lead는 1~2문장, points는 3~5개로 작성해.
            - 각 point는 너무 길지 않게 1~2문장으로 작성해.
            - 추천 점수는 "서울시 기준 상위 약 n%" 표현을 포함해 설명해.
            - 사용자가 바로 이해할 수 있는 쉬운 설명으로 풀어 써.
            - 아래 [RAG 검색 근거]에 있는 문서 내용을 동네 분위기 근거로 사용해.
            - RAG 문서가 있으면 "생활 정보가 확인됩니다"처럼 빈 문장으로 쓰지 말고, 문서에 나온 구체적인 시설·상권·공원·교육·의료·거래 특징을 1개 이상 언급해.
            - RAG 문서 기반 분위기는 "상권 선택지가 많은 편"처럼 내용이 드러나게 작성해. 또한 RAG 문서의 데이터를 활용해서 너가 동네를 분석해.
            - RAG 문서가 없으면 "문서 기반 분위기 정보는 확인되지 않았다"고 솔직하게 말해.
            - 생활환경을 과장하지 말고 "편입니다", "볼 수 있습니다", "확인이 필요합니다"처럼 신중하게 표현해.
            - 그리고 답변의 가장 상위에 동네의 요약을 간단히 작성해.

            [동네]
            이름: %s
            거래 유형: %s
            예산 조건: %s
            조건 일치 매물 수: %d건
            총 추천 점수: %s

            [카테고리 점수]
            %s
            [RAG 검색 근거]
            %s
            [규칙 기반 fallback 예시]
            lead: %s
            points: %s
            caution: %s
            """.formatted(
            fullName(neighborhood),
            nullToEmpty(request.getDealType()),
            nullToEmpty(request.getBudgetLabel()),
            request.getHouseCount(),
            formatNumber(neighborhood.getScore()),
            scoreLines,
            ragLines,
            fallback.getLead(),
            fallback.getPoints(),
            fallback.getCaution()
        );
    }

    private NeighborhoodSummaryResponse parseLlmResponse(String content, boolean ragDocumentFound) {
        if (isBlank(content)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(stripJsonFence(content));
            String lead = root.path("lead").asText("");
            String caution = root.path("caution").asText("");
            List<String> points = new java.util.ArrayList<>();
            JsonNode pointNodes = root.path("points");
            if (pointNodes.isArray()) {
                for (JsonNode pointNode : pointNodes) {
                    String point = pointNode.asText("");
                    if (!isBlank(point)) {
                        points.add(point.trim());
                    }
                }
            }
            if (isBlank(lead) || points.isEmpty()) {
                return null;
            }
            return new NeighborhoodSummaryResponse(
                lead.trim(),
                points,
                ragDocumentFound,
                isBlank(caution) ? "AI 요약은 추천 점수와 공개 데이터 근거를 바탕으로 만든 해석입니다." : caution.trim()
            );
        } catch (Exception exception) {
            log.info("[NeighborhoodSummary] LLM response parse failed. content={}", content);
            return null;
        }
    }

    private List<CategoryScore> safeScores(RecommendedNeighborhood neighborhood) {
        if (neighborhood.getCategoryScores() == null) {
            return List.of();
        }
        return neighborhood.getCategoryScores().stream()
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * RAG 검색에 사용할 관심 키워드를 만든다.
     * 카테고리 코드 그대로보다 "교통", "장보기" 같은 생활어를 넣어야 문서 검색이 잘 맞는다.
     */
    private List<String> focusKeywords(List<CategoryScore> scores, List<String> priorities) {
        List<String> keywords = new java.util.ArrayList<>();
        List<String> categoryCodes = priorities == null || priorities.isEmpty()
            ? scores.stream().map(CategoryScore::getCategory).toList()
            : priorities;
        for (String category : categoryCodes) {
            String keyword = switch (nullToEmpty(category)) {
                case "TRANSPORT" -> "교통";
                case "SCHOOL" -> "학교";
                case "CAFE" -> "카페";
                case "CULTURE" -> "문화";
                case "SHOPPING" -> "장보기";
                case "PARK" -> "공원";
                case "MEDICAL" -> "의료";
                case "PET" -> "반려동물";
                default -> null;
            };
            if (keyword != null && !keywords.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    private String buildSearchQuestion(
        RecommendedNeighborhood neighborhood,
        List<String> focusKeywords,
        NeighborhoodSummaryRequest request
    ) {
        return String.join(" ",
            nullToEmpty(neighborhood.getSigunguName()),
            nullToEmpty(neighborhood.getDongName()),
            String.join(" ", focusKeywords),
            nullToEmpty(request.getDealType()),
            nullToEmpty(request.getBudgetLabel())
        ).trim();
    }

    private String buildLead(
        RecommendedNeighborhood neighborhood,
        List<CategoryScore> scores,
        NeighborhoodSummaryRequest request
    ) {
        String fullName = fullName(neighborhood);
        String topLabel = scores.isEmpty() ? "생활 조건" : categoryLabel(scores.get(0).getCategory());
        String dealText = isBlank(request.getDealType()) ? "선택한 거래 조건" : request.getDealType();

        return "%s은 %s 조건에서 %s 항목이 가장 두드러지는 동네예요."
            .formatted(fullName, dealText, topLabel, topLabel);
    }

    private String scorePoint(CategoryScore score) {
        String label = categoryLabel(score.getCategory());
        int topPercent = topPercent(score.getScore());
        return "%s은 서울시 기준 상위 약 %d%% 수준입니다. %s"
            .formatted(label, topPercent, plainCategoryExplanation(score.getCategory(), score.getScore()));
    }

    private void addHousePoint(List<String> points, NeighborhoodSummaryRequest request) {
        int houseCount = request == null ? 0 : request.getHouseCount();
        String budgetLabel = request == null ? "" : nullToEmpty(request.getBudgetLabel());
        if (houseCount > 0) {
            points.add("현재 조건에 맞는 매물은 %d건 확인됩니다. 생활 점수가 높더라도 실제 선택 가능한 매물 수가 중요하므로, 가격과 위치를 함께 비교해 보는 것이 좋습니다."
                .formatted(houseCount));
            return;
        }
        points.add("현재 조건에 맞는 매물은 확인되지 않았습니다. 생활 점수가 좋아도 %s 조건의 실제 선택지는 따로 확인이 필요합니다."
            .formatted(isBlank(budgetLabel) ? "현재 예산" : budgetLabel));
    }

    private void addRagPoint(List<String> points, List<RagSearchResult> ragResults) {
        if (ragResults == null || ragResults.isEmpty()) {
            points.add("RAG 문서에서 이 동네와 직접 맞물리는 추가 생활 문서는 찾지 못해, 추천 점수 중심으로 해석했습니다.");
            return;
        }

        RagSearchResult result = ragResults.get(0);
        points.add("RAG 문서의 %s 항목에서는 %s"
            .formatted(result.sectionTitle(), naturalRagSummary(result.content())));
    }

    /**
     * RAG 검색 결과 원문을 화면용 문장으로 다듬는다.
     *
     * <p>이 함수의 목적은 "RAG 문서가 있다"는 사실만 말하는 것이 아니라, 문서에 실제로 어떤 생활
     * 특징이 적혀 있었는지 사용자가 바로 볼 수 있게 만드는 것이다. 원문 밖의 시설명이나 분위기는
     * 새로 만들지 않는다.</p>
     */
    private String naturalRagSummary(String content) {
        if (isBlank(content)) {
            return "구체적인 생활 정보가 짧게 확인됩니다.";
        }
        String normalized = content
            .replaceAll("\\s+", " ")
            .replace("입니다.", "인 것으로 정리되어 있습니다.")
            .replace("습니다.", "는 내용이 확인됩니다.")
            .trim();
        if (normalized.length() > 150) {
            normalized = normalized.substring(0, 150).trim() + "...";
        }
        if (normalized.endsWith(".") || normalized.endsWith("다.")) {
            return normalized;
        }
        return normalized + "는 내용이 확인됩니다.";
    }

    private String plainCategoryExplanation(String category, double score) {
        String strength = score >= 80.0 ? "강점으로 볼 수 있습니다."
            : score >= 60.0 ? "비교적 양호한 편입니다."
            : score >= 40.0 ? "보통 수준으로 해석할 수 있습니다."
            : "상대적으로 약한 항목일 수 있습니다.";
        return switch (nullToEmpty(category)) {
            case "TRANSPORT" -> "대중교통 관련 시설 점수를 기준으로 이동 선택지가 %s".formatted(strength);
            case "SHOPPING" -> "장보기와 일상 편의시설 이용 측면에서 %s".formatted(strength);
            case "PET" -> "반려동물 생활에 필요한 공원·동물병원·반려용품 지표를 종합하면 %s".formatted(strength);
            case "SCHOOL" -> "동네 내부의 학교 수를 기준으로 교육시설 접근성은 %s".formatted(strength);
            case "PARK" -> "공원과 산책 환경을 기준으로 휴식 공간 선택지는 %s".formatted(strength);
            case "MEDICAL" -> "병원과 의료 관련 시설 지표를 기준으로 의료 인프라는 %s".formatted(strength);
            case "CAFE" -> "카페와 외식 업소 수를 기준으로 외식·모임 선택지는 %s".formatted(strength);
            case "CULTURE" -> "운동·문화 관련 시설 지표를 기준으로 여가 활동 선택지는 %s".formatted(strength);
            default -> "선택한 생활 우선순위 안에서 이 항목은 %s".formatted(strength);
        };
    }

    private int topPercent(double score) {
        return Math.max(1, Math.min(100, (int) Math.round(100.0 - score)));
    }

    private String stripJsonFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```json")) {
            return trimmed.substring("```json".length()).replaceFirst("```\\s*$", "").trim();
        }
        if (trimmed.startsWith("```")) {
            return trimmed.substring("```".length()).replaceFirst("```\\s*$", "").trim();
        }
        return trimmed;
    }

    private String trimForPrompt(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String categoryLabel(String category) {
        return CATEGORY_LABELS.getOrDefault(category, isBlank(category) ? "생활환경" : category);
    }

    private String fullName(RecommendedNeighborhood neighborhood) {
        return String.join(" ", List.of(
            nullToEmpty(neighborhood.getSidoName()),
            nullToEmpty(neighborhood.getSigunguName()),
            nullToEmpty(neighborhood.getDongName())
        )).trim().replaceAll(" +", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
