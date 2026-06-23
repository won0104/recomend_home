package com.ssafy.prj.chat.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.chat.dto.LifestyleParseResult;

@Service
public class NeighborhoodChatParser {
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern RANK_CHANGE = Pattern.compile("([123])\\s*순위");
    private static final Pattern MINUTES = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*분");

    private final ObjectMapper objectMapper;
    private final org.springframework.ai.chat.client.ChatClient chatClient;

    public NeighborhoodChatParser(ObjectMapper objectMapper,
            ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> chatClientBuilderProvider) {
        this.objectMapper = objectMapper;
        org.springframework.ai.chat.client.ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
    }

    public String parsePlaceKeyword(String message) {
        String value = clean(message);
        return value.replace("근처", "")
                .replace("주변", "")
                .replace("기준", "")
                .replace("에서", "")
                .replace("으로", "")
                .replace("로", "")
                .trim();
    }

    public Integer parseSelectionIndex(String message, int size) {
        Matcher matcher = NUMBER.matcher(message == null ? "" : message);
        if (matcher.find()) {
            int index = (int) Double.parseDouble(matcher.group(1)) - 1;
            return index >= 0 && index < size ? index : null;
        }
        if (message != null && (message.contains("첫") || message.contains("일번") || message.contains("첫번째"))) {
            return size > 0 ? 0 : null;
        }
        return null;
    }

    public Double parseRadiusKm(String message) {
        if (message == null) {
            return null;
        }
        String lower = message.toLowerCase(Locale.ROOT)
                .replace("키로미터", "km")
                .replace("킬로미터", "km")
                .replace("키로", "km")
                .replace("킬로", "km")
                .replace("정도", "")
                .replace("쯤", "");

        Matcher minuteMatcher = MINUTES.matcher(lower);
        if (minuteMatcher.find()) {
            double minutes = Double.parseDouble(minuteMatcher.group(1));
            if (lower.contains("도보") || lower.contains("걸어서") || lower.contains("걷")) {
                return Math.max(0.5, minutes * 0.08);
            }
            if (lower.contains("차") || lower.contains("운전") || lower.contains("자동차")) {
                return Math.max(1.0, minutes * 0.35);
            }
            if (lower.contains("버스") || lower.contains("지하철") || lower.contains("대중교통")) {
                return Math.max(1.0, minutes * 0.25);
            }
            return Math.max(1.0, minutes * 0.25);
        }

        Matcher matcher = NUMBER.matcher(lower);
        if (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            if (lower.contains("m") && !lower.contains("km")) {
                return Math.max(0.1, value / 1000.0);
            }
            return value;
        }
        if (lower.contains("도보") || lower.contains("걸어서")) {
            return 1.5;
        }
        if (lower.contains("차") || lower.contains("운전")) {
            return 5.0;
        }
        return null;
    }

    public Integer parseBudgetMaxPrice(String message) {
        if (message == null) {
            return null;
        }
        String value = message.replace(",", "")
                .replace(" ", "")
                .replace("정도", "")
                .replace("쯤", "");
        if (value.contains("상관없") || value.contains("무관") || value.contains("제한없")) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        double number = Double.parseDouble(matcher.group(1));
        if (value.contains("억")) {
            return (int) Math.round(number * 10000);
        }
        if (value.contains("천만")) {
            return (int) Math.round(number * 1000);
        }
        if (value.contains("만")) {
            return (int) Math.round(number);
        }
        return (int) Math.round(number * 10000);
    }

    public LifestyleParseResult parseLifestyle(String message) {
        LifestyleParseResult aiResult = parseLifestyleWithAi(message);
        if (!aiResult.getPriorities().isEmpty()) {
            aiResult.setPriorities(limit(aiResult.getPriorities(), 3));
            return aiResult;
        }

        LifestyleParseResult result = new LifestyleParseResult();
        result.setPriorities(limit(parseLifestyleByRule(message), 3));
        result.setAmbiguous(result.getPriorities().isEmpty());
        return result;
    }

    public Integer parseRankToChange(String message) {
        Matcher matcher = RANK_CHANGE.matcher(message == null ? "" : message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) - 1;
        }
        return null;
    }

    public boolean isAffirmative(String message) {
        if (message == null) {
            return false;
        }
        String value = message.trim().toLowerCase(Locale.ROOT);
        return value.equals("응") || value.equals("네") || value.equals("예") || value.equals("ㅇㅇ")
                || value.contains("맞아") || value.contains("좋아") || value.contains("이대로")
                || value.contains("진행") || value.contains("추천");
    }

    public boolean isCorrectionRequest(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("다시") || message.contains("수정") || message.contains("바꿔")
                || message.contains("변경") || message.contains("잘못") || message.contains("실수")
                || message.contains("돌아");
    }

    public String correctionTarget(String message) {
        if (message == null) {
            return "";
        }
        if (message.contains("위치") || message.contains("장소") || message.contains("기준")) {
            return "place";
        }
        if (message.contains("반경") || message.contains("거리") || message.contains("km")
                || message.contains("키로") || message.contains("킬로")) {
            return "radius";
        }
        if (message.contains("예산") || message.contains("가격") || message.contains("금액")
                || message.contains("억") || message.contains("만원")) {
            return "budget";
        }
        if (message.contains("생활") || message.contains("환경") || message.contains("우선")
                || message.contains("순위")) {
            return "lifestyle";
        }
        return "";
    }

    public boolean isHelpQuestion(String message) {
        if (message == null) {
            return false;
        }
        String value = message.trim();
        boolean questionLike = value.contains("뭐") || value.contains("무슨") || value.contains("어떻게")
                || value.contains("기준") || value.contains("의미") || value.contains("설명");
        boolean onlyValueQuestion = NUMBER.matcher(value).find()
                && (value.contains("km") || value.contains("키로") || value.contains("킬로")
                || value.contains("억") || value.contains("만원"));
        return questionLike && !onlyValueQuestion;
    }

    private LifestyleParseResult parseLifestyleWithAi(String message) {
        LifestyleParseResult empty = new LifestyleParseResult();
        if (chatClient == null || message == null || message.isBlank()) {
            return empty;
        }
        try {
            String content = chatClient.prompt()
                    .system("""
                            You extract neighborhood lifestyle priorities from Korean text.
                            Return JSON only. Do not include markdown.
                            Allowed values: SCHOOL, CAFE, CULTURE, SHOPPING, PARK, MEDICAL, PET.
                            Preserve the user's priority order when it is clear.
                            Schema: {"priorities":["PARK"],"ambiguous":false}
                            """)
                    .user(message)
                    .call()
                    .content();
            JsonNode root = objectMapper.readTree(content);
            List<String> priorities = new ArrayList<>();
            if (root.has("priorities")) {
                for (JsonNode node : root.get("priorities")) {
                    String category = normalizeCategory(node.asText());
                    if (category != null && !priorities.contains(category)) {
                        priorities.add(category);
                    }
                }
            }
            empty.setPriorities(priorities);
            empty.setAmbiguous(root.path("ambiguous").asBoolean(priorities.isEmpty()));
            return empty;
        } catch (Exception e) {
            return empty;
        }
    }

    private List<String> parseLifestyleByRule(String message) {
        String value = clean(message);
        Set<String> categories = new LinkedHashSet<>();
        addIfContains(categories, value, "SCHOOL", "학교", "학원", "교육", "초등", "중학교", "고등");
        addIfContains(categories, value, "CAFE", "카페", "외식", "음식", "식당", "맛집");
        addIfContains(categories, value, "CULTURE", "운동", "문화", "헬스", "체육", "영화", "공연", "여가");
        addIfContains(categories, value, "SHOPPING", "마트", "쇼핑", "장보기", "편의", "시장", "백화점", "생활");
        addIfContains(categories, value, "PARK", "공원", "산책", "녹지", "자연");
        addIfContains(categories, value, "MEDICAL", "병원", "의료", "약국", "의원");
        addIfContains(categories, value, "PET", "반려", "강아지", "고양이", "펫", "동물");
        return new ArrayList<>(categories);
    }

    private void addIfContains(Set<String> categories, String message, String category, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                categories.add(category);
                return;
            }
        }
    }

    private List<String> limit(List<String> values, int limit) {
        return values.stream()
                .map(this::normalizeCategory)
                .filter(v -> v != null)
                .distinct()
                .limit(limit)
                .toList();
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return null;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "SCHOOL", "CAFE", "CULTURE", "SHOPPING", "PARK", "MEDICAL", "PET" -> upper;
            default -> null;
        };
    }

    private String clean(String message) {
        return message == null ? "" : message.trim();
    }
}
