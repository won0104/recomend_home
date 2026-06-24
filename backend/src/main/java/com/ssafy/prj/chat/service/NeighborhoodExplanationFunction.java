package com.ssafy.prj.chat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.ssafy.prj.chat.dto.ChatSessionState;
import com.ssafy.prj.chat.dto.NeighborhoodExplanationResult;
import com.ssafy.prj.chat.dto.NeighborhoodExplanationSlot;
import com.ssafy.prj.model.dto.neighborhood.CategoryScore;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

import lombok.RequiredArgsConstructor;

@Component("explainNeighborhoodWithRag")
@Description("최근 추천된 동네 중 하나를 골라 generated/rag/seoul-gugun 문서와 추천 점수 근거로 상세 설명합니다.")
@RequiredArgsConstructor
public class NeighborhoodExplanationFunction implements Function<NeighborhoodExplanationSlot, NeighborhoodExplanationResult> {
    private static final Pattern RANK_PATTERN = Pattern.compile("([1-3])\\s*위|첫\\s*번째|두\\s*번째|세\\s*번째");
    private static final List<String> FOCUS_CANDIDATES = List.of(
        "임산부", "육아", "교육", "의료", "병원", "학교", "공원", "교통", "상권", "조용함",
        "카페", "외식", "문화", "운동", "장보기", "반려동물", "월세", "전세", "매매"
    );

    private final ChatStateService chatStateService;
    private final ChatSessionContext chatSessionContext;
    private final RagSearchService ragSearchService;

    @Override
    public NeighborhoodExplanationResult apply(NeighborhoodExplanationSlot slot) {
        ChatSessionContext.CurrentChatSession current = chatSessionContext.get();
        if (current == null || current.sessionId() == null) {
            return emptyResult(slot, "최근 추천 세션을 찾지 못했어요. 먼저 동네 추천을 한 번 받아주세요.");
        }

        ChatSessionState state = chatStateService.getState(current.sessionId());
        List<RecommendedNeighborhood> recommendations = state.getLastRecommendations();
        if (recommendations == null || recommendations.isEmpty()) {
            return emptyResult(slot, "최근 추천 결과가 없어서 상세 설명을 만들 수 없어요. 먼저 동네 추천을 받아주세요.");
        }

        RecommendedNeighborhood target = findTarget(slot, recommendations)
            .orElseGet(() -> recommendations.stream()
                .min(Comparator.comparingInt(RecommendedNeighborhood::getRank))
                .orElse(recommendations.get(0)));

        List<String> focuses = mergeFocusKeywords(slot);
        List<RagSearchResult> ragResults = ragSearchService.search(
            slot == null ? "" : slot.question(),
            target.getSigunguName(),
            target.getDongName(),
            focuses
        );
        String answer = buildAnswer(target, focuses, ragResults);

        return new NeighborhoodExplanationResult(target, focuses, answer, !ragResults.isEmpty());
    }

    private NeighborhoodExplanationResult emptyResult(NeighborhoodExplanationSlot slot, String answer) {
        return new NeighborhoodExplanationResult(null, mergeFocusKeywords(slot), answer, false);
    }

    private Optional<RecommendedNeighborhood> findTarget(
        NeighborhoodExplanationSlot slot,
        List<RecommendedNeighborhood> recommendations
    ) {
        Integer rank = slot == null ? null : slot.rank();
        if (rank == null && slot != null) {
            rank = extractRank(slot.question());
        }
        if (rank != null) {
            int targetRank = rank;
            Optional<RecommendedNeighborhood> byRank = recommendations.stream()
                .filter(recommendation -> recommendation.getRank() == targetRank)
                .findFirst();
            if (byRank.isPresent()) {
                return byRank;
            }
        }

        String question = slot == null ? "" : nullToEmpty(slot.question());
        String dongName = slot == null ? "" : nullToEmpty(slot.dongName());
        String sigunguName = slot == null ? "" : nullToEmpty(slot.sigunguName());
        return recommendations.stream()
            .filter(recommendation ->
                containsName(dongName, recommendation.getDongName())
                    || containsName(sigunguName, recommendation.getSigunguName())
                    || containsName(question, recommendation.getDongName())
                    || containsName(question, recommendation.getSigunguName()))
            .findFirst();
    }

    private Integer extractRank(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = RANK_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        if (matcher.group(1) != null) {
            return Integer.parseInt(matcher.group(1));
        }
        String match = matcher.group();
        if (match.contains("첫")) {
            return 1;
        }
        if (match.contains("두")) {
            return 2;
        }
        if (match.contains("세")) {
            return 3;
        }
        return null;
    }

    private boolean containsName(String text, String name) {
        return text != null && name != null && !name.isBlank() && text.contains(name);
    }

    private List<String> mergeFocusKeywords(NeighborhoodExplanationSlot slot) {
        List<String> focuses = new ArrayList<>();
        // LLM이 사용자가 말하지 않은 관심사를 추론해 focusKeywords에 넣을 수 있다.
        // 그래서 tool 인자의 focusKeywords는 신뢰하지 않고, 실제 사용자 질문에 포함된 단어만 focus로 인정한다.
        String question = slot == null ? "" : nullToEmpty(slot.question());
        String lower = question.toLowerCase(Locale.ROOT);
        for (String candidate : FOCUS_CANDIDATES) {
            if (lower.contains(candidate.toLowerCase(Locale.ROOT))) {
                addFocus(focuses, candidate);
            }
        }
        return focuses;
    }

    private void addFocus(List<String> focuses, String focus) {
        if (focus != null && !focus.isBlank() && !focuses.contains(focus.trim())) {
            focuses.add(focus.trim());
        }
    }

    private String buildAnswer(
        RecommendedNeighborhood target,
        List<String> focuses,
        List<RagSearchResult> ragResults
    ) {
        String fullName = target.getSidoName() + " " + target.getSigunguName() + " " + target.getDongName();
        StringBuilder answer = new StringBuilder();
        answer.append("🏡 ").append(fullName).append(" 상세 설명\n\n");
        answer.append("추천 점수는 ").append(formatScore(target.getScore())).append("점이에요.");
        if (!focuses.isEmpty()) {
            answer.append(" 이번 답변은 ").append(String.join(", ", focuses)).append(" 관점으로 볼게요.");
        }
        answer.append("\n\n");

        appendScoreEvidence(answer, target.getCategoryScores());
        appendRagEvidence(answer, ragResults);

        if (focuses.stream().anyMatch(focus -> focus.contains("임산부") || focus.contains("의료") || focus.contains("병원"))) {
            answer.append("\n의료 관련 내용은 병원 접근성 같은 생활 인프라 관점의 참고 정보예요. 개인 건강 판단이나 진료 선택은 의료 전문가와 확인해 주세요.\n");
        }
        answer.append("\n더 비교해보고 싶으면 다른 순위나 동 이름을 말해 주세요.");
        return answer.toString();
    }

    private void appendScoreEvidence(StringBuilder answer, List<CategoryScore> categoryScores) {
        if (categoryScores == null || categoryScores.isEmpty()) {
            return;
        }
        answer.append("✅ 추천 점수 근거\n");
        categoryScores.stream()
            .sorted(Comparator.comparingDouble(CategoryScore::getContribution).reversed())
            .limit(3)
            .forEach(score -> answer.append("- ")
                .append(score.getCategory())
                .append(": 점수 ")
                .append(formatScore(score.getScore()))
                .append(", 반영 기여 ")
                .append(formatScore(score.getContribution()))
                .append('\n'));
        answer.append('\n');
    }

    private void appendRagEvidence(StringBuilder answer, List<RagSearchResult> ragResults) {
        if (ragResults == null || ragResults.isEmpty()) {
            answer.append("📄 RAG 문서\n");
            answer.append("- 관련 문서 chunk를 찾지 못해서, 추천 점수 근거 중심으로만 설명할게요.\n");
            return;
        }

        boolean embeddingBased = ragResults.stream().anyMatch(RagSearchResult::embeddingBased);
        answer.append(embeddingBased ? "📄 문서 유사도 검색으로 확인한 생활 정보\n" : "📄 문서 키워드 검색으로 확인한 생활 정보\n");
        for (RagSearchResult result : ragResults) {
            answer.append("- ")
                .append(result.sectionTitle())
                .append(": ")
                .append(result.content())
                .append('\n');
        }
    }

    private String formatScore(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
