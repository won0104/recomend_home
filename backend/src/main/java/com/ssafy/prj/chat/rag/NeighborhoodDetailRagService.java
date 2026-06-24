package com.ssafy.prj.chat.rag;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class NeighborhoodDetailRagService {

    private static final Pattern RANK_PATTERN = Pattern.compile("(\\d+)\\s*(?:위|번째)");
    private static final List<String> MODIFY_KEYWORDS = List.of(
        "다시", "새로", "바꿔", "변경", "예산", "반경", "km", "매매", "전세", "월세", "아파트", "오피스텔", "우선순위"
    );
    private static final List<String> DETAIL_KEYWORDS = List.of(
        "자세", "상세", "설명", "소개", "궁금", "알려", "어때", "어떤", "뭐가", "특징",
        "분위기", "장점", "단점", "교육", "학교", "공원", "병원", "의료", "문화",
        "편의", "상권", "카페", "반려", "아이", "살기", "생활", "치안"
    );

    private final LastRecommendationStore lastRecommendationStore;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ChatRagProperties properties;

    public NeighborhoodDetailRagService(
        LastRecommendationStore lastRecommendationStore,
        VectorStore vectorStore,
        ChatClient.Builder chatClientBuilder,
        ChatRagProperties properties
    ) {
        this.lastRecommendationStore = lastRecommendationStore;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                You answer neighborhood detail questions using only the provided retrieved context.
                Answer in Korean, be concise but useful, and include both strengths and cautions when context supports it.
                If the context is insufficient, say what is missing instead of inventing facts.
                """)
            .build();
        this.properties = properties;
    }

    public Optional<String> answerIfDetailQuestion(String sessionId, String userMessage) {
        if (isBlank(userMessage) || looksLikeRecommendationChange(userMessage)) {
            return Optional.empty();
        }

        Optional<NeighborhoodRecommendationSnapshot> snapshotOptional = lastRecommendationStore.find(sessionId);
        if (snapshotOptional.isEmpty()) {
            return Optional.empty();
        }

        NeighborhoodRecommendationSnapshot snapshot = snapshotOptional.get();
        Optional<RecommendedDongSnapshot> targetOptional = resolveTarget(snapshot, userMessage);
        if (targetOptional.isEmpty() || !looksLikeDetailQuestion(snapshot, userMessage)) {
            return Optional.empty();
        }

        RecommendedDongSnapshot target = targetOptional.get();
        List<Document> documents = searchDocuments(target, userMessage);
        if (documents.isEmpty()) {
            return Optional.of(target.fullName() + "에 대한 상세 자료가 아직 등록되어 있지 않습니다. 동네 성향 자료를 먼저 embedding 해주세요.");
        }

        String context = buildContext(documents);
        String answer = chatClient.prompt()
            .user("""
                [대상 동네]
                %s

                [사용자 질문]
                %s

                [검색된 동네 성향 자료]
                %s
                """.formatted(target.fullName(), userMessage.trim(), context))
            .call()
            .content();
        return Optional.ofNullable(answer);
    }

    private List<Document> searchDocuments(RecommendedDongSnapshot target, String userMessage) {
        String query = target.fullName() + " " + userMessage;
        List<Document> searched = vectorStore.similaritySearch(SearchRequest.builder()
            .query(query)
            .topK(Math.max(properties.getSearchTopK(), 3))
            .build());

        List<Document> exactDongDocuments = searched.stream()
            .filter(document -> metadataEquals(document, "dong", target.dongName()))
            .sorted(Comparator.comparing(document -> document.getScore() == null ? 0.0 : document.getScore(), Comparator.reverseOrder()))
            .toList();
        return exactDongDocuments.isEmpty() ? searched : exactDongDocuments;
    }

    private boolean metadataEquals(Document document, String key, String expected) {
        Object value = document.getMetadata().get(key);
        return value != null && expected != null && expected.equals(String.valueOf(value));
    }

    private String buildContext(List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            String category = String.valueOf(document.getMetadata().getOrDefault("category", "PROFILE"));
            builder.append("[").append(category).append("]\n")
                .append(document.getText()).append("\n\n");
        }
        return builder.toString();
    }

    private Optional<RecommendedDongSnapshot> resolveTarget(NeighborhoodRecommendationSnapshot snapshot, String userMessage) {
        Matcher rankMatcher = RANK_PATTERN.matcher(userMessage);
        if (rankMatcher.find()) {
            int rank = Integer.parseInt(rankMatcher.group(1));
            return snapshot.recommendations().stream()
                .filter(recommendation -> recommendation.rank() == rank)
                .findFirst();
        }

        String normalized = userMessage.toLowerCase(Locale.ROOT);
        return snapshot.recommendations().stream()
            .filter(recommendation -> containsIgnoreCase(normalized, recommendation.dongName())
                || containsIgnoreCase(normalized, recommendation.sigunguName() + " " + recommendation.dongName())
                || normalized.contains("그 동네")
                || normalized.contains("거기")
                || normalized.contains("첫 번째")
                || normalized.contains("첫번째"))
            .findFirst();
    }

    private boolean looksLikeRecommendationChange(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return MODIFY_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean looksLikeDetailQuestion(NeighborhoodRecommendationSnapshot snapshot, String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return DETAIL_KEYWORDS.stream().anyMatch(normalized::contains)
            || snapshot.recommendations().stream().anyMatch(recommendation ->
                containsIgnoreCase(normalized, recommendation.dongName())
                    || containsIgnoreCase(normalized, recommendation.sigunguName() + " " + recommendation.dongName()))
            || RANK_PATTERN.matcher(message).find()
            || normalized.contains("첫 번째")
            || normalized.contains("첫번째")
            || normalized.contains("그 동네")
            || normalized.contains("거기");
    }

    private boolean containsIgnoreCase(String normalizedSource, String value) {
        return value != null && normalizedSource.contains(value.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
