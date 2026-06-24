package com.ssafy.prj.chat.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagSearchService {
    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);
    private static final Pattern DONG_HEADING_PATTERN = Pattern.compile("^###\\s+(.+?)\\s*\\((\\d{10})\\)\\s*$");
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("^####\\s+(.+?)\\s*$");
    private static final Map<String, List<String>> FOCUS_TERMS = Map.ofEntries(
        Map.entry("의료", List.of("의료", "병원", "보건", "약국")),
        Map.entry("병원", List.of("의료", "병원", "보건", "약국")),
        Map.entry("교육", List.of("교육", "학교", "초중등학교", "학생", "학급")),
        Map.entry("학교", List.of("교육", "학교", "초중등학교", "학생", "학급")),
        Map.entry("육아", List.of("학교", "교육", "공원", "의료", "보건", "문화")),
        Map.entry("임산부", List.of("의료", "병원", "보건", "공원", "문화")),
        Map.entry("공원", List.of("공원", "산책", "녹지")),
        Map.entry("상권", List.of("상권", "카페", "음식점", "마트", "슈퍼마켓", "편의점", "시장")),
        Map.entry("카페", List.of("카페", "외식", "음식점")),
        Map.entry("장보기", List.of("마트", "슈퍼마켓", "편의점", "시장", "장보기")),
        Map.entry("반려동물", List.of("동물병원", "반려동물", "펫"))
    );

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final Map<String, List<RagChunk>> chunksBySigungu = new ConcurrentHashMap<>();

    @Value("${chat.rag.seoul-gugun-dir:}")
    private String configuredRagDirectory;

    @Value("${chat.rag.embedding.enabled:true}")
    private boolean embeddingEnabled;

    @Value("${chat.rag.search.top-k:7}")
    private int topK;

    public RagSearchService(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    public List<RagSearchResult> search(
        String question,
        String sigunguName,
        String dongName,
        List<String> focusKeywords
    ) {
        List<RagChunk> chunks = loadChunks(sigunguName);
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<RagChunk> dongScopedChunks = chunks.stream()
            .filter(chunk -> isBlank(dongName) || dongName.equals(chunk.dongName()))
            .toList();
        List<RagChunk> scopedChunks = dongScopedChunks.isEmpty() ? chunks : dongScopedChunks;

        List<String> searchTerms = expandSearchTerms(question, focusKeywords);
        Optional<List<RagSearchResult>> embeddingResults = searchByEmbedding(question, scopedChunks, searchTerms);
        return embeddingResults.orElseGet(() -> searchByKeyword(scopedChunks, searchTerms));
    }

    private Optional<List<RagSearchResult>> searchByEmbedding(
        String question,
        List<RagChunk> chunks,
        List<String> searchTerms
    ) {
        if (!embeddingEnabled || isBlank(question)) {
            return Optional.empty();
        }

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return Optional.empty();
        }

        try {
            float[] queryEmbedding = embeddingModel.embed(buildEmbeddingQuery(question, searchTerms));
            for (RagChunk chunk : chunks) {
                if (chunk.embedding() == null) {
                    chunk.setEmbedding(embeddingModel.embed(chunk.embeddingText()));
                }
            }
            return Optional.of(chunks.stream()
                .map(chunk -> new RagSearchResult(
                    chunk.sigunguName(),
                    chunk.dongName(),
                    chunk.sectionTitle(),
                    chunk.content(),
                    cosineSimilarity(queryEmbedding, chunk.embedding()),
                    true
                ))
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .limit(Math.max(1, topK))
                .toList());
        } catch (Exception exception) {
            log.info("RAG embedding search failed. fallback to keyword search. reason={}", exception.getMessage());
            return Optional.empty();
        }
    }

    private List<RagSearchResult> searchByKeyword(List<RagChunk> chunks, List<String> searchTerms) {
        return chunks.stream()
            .map(chunk -> new RagSearchResult(
                chunk.sigunguName(),
                chunk.dongName(),
                chunk.sectionTitle(),
                chunk.content(),
                keywordScore(chunk, searchTerms),
                false
            ))
            .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private List<RagChunk> loadChunks(String sigunguName) {
        if (isBlank(sigunguName)) {
            return List.of();
        }
        return chunksBySigungu.computeIfAbsent(sigunguName, this::readAndChunkSigunguDocument);
    }

    private List<RagChunk> readAndChunkSigunguDocument(String sigunguName) {
        Optional<Path> path = findRagDocument(sigunguName);
        if (path.isEmpty()) {
            return List.of();
        }
        try {
            return parseChunks(sigunguName, Files.readString(path.get(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            log.warn("RAG document read failed. sigunguName={}, path={}", sigunguName, path.get(), exception);
            return List.of();
        }
    }

    private Optional<Path> findRagDocument(String sigunguName) {
        for (Path base : candidateRagDirectories()) {
            Path path = base.resolve(sigunguName + ".md").normalize();
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    private List<Path> candidateRagDirectories() {
        List<Path> candidates = new ArrayList<>();
        if (!isBlank(configuredRagDirectory)) {
            candidates.add(Path.of(configuredRagDirectory));
        }
        Path userDir = Path.of(System.getProperty("user.dir"));
        candidates.add(userDir.resolve("generated/rag/seoul-gugun"));
        candidates.add(userDir.resolve("../generated/rag/seoul-gugun"));
        return candidates;
    }

    private List<RagChunk> parseChunks(String sigunguName, String document) {
        List<RagChunk> chunks = new ArrayList<>();
        String currentDong = null;
        String currentSection = "요약";

        for (String rawLine : document.split("\\R")) {
            String line = rawLine.trim();
            Matcher dongMatcher = DONG_HEADING_PATTERN.matcher(line);
            if (dongMatcher.matches()) {
                currentDong = dongMatcher.group(1);
                currentSection = "요약";
                continue;
            }

            Matcher sectionMatcher = SECTION_HEADING_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                currentSection = sectionMatcher.group(1);
                continue;
            }

            if (currentDong == null || !line.startsWith("* ")) {
                continue;
            }

            String content = line.substring(2).trim();
            if (content.isBlank() || content.contains("거래 평균은")) {
                continue;
            }
            chunks.add(new RagChunk(sigunguName, currentDong, currentSection, content));
        }
        return chunks;
    }

    private List<String> expandSearchTerms(String question, List<String> focusKeywords) {
        Set<String> terms = new HashSet<>();
        if (!isBlank(question)) {
            for (String token : question.replaceAll("[^가-힣A-Za-z0-9\\s]", " ").split("\\s+")) {
                if (token.length() >= 2) {
                    terms.add(token.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (focusKeywords != null) {
            for (String focus : focusKeywords) {
                if (!isBlank(focus)) {
                    terms.add(focus.toLowerCase(Locale.ROOT));
                    FOCUS_TERMS.getOrDefault(focus, List.of()).forEach(term -> terms.add(term.toLowerCase(Locale.ROOT)));
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private String buildEmbeddingQuery(String question, List<String> searchTerms) {
        if (searchTerms.isEmpty()) {
            return question;
        }
        return question + "\n관심 키워드: " + String.join(", ", searchTerms);
    }

    private double keywordScore(RagChunk chunk, List<String> searchTerms) {
        if (searchTerms.isEmpty()) {
            return 0.0;
        }
        String text = chunk.embeddingText().toLowerCase(Locale.ROOT);
        double score = 0.0;
        for (String term : searchTerms) {
            if (text.contains(term)) {
                score += 1.0;
            }
        }
        return score;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0.0;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class RagChunk {
        private final String sigunguName;
        private final String dongName;
        private final String sectionTitle;
        private final String content;
        private float[] embedding;

        private RagChunk(String sigunguName, String dongName, String sectionTitle, String content) {
            this.sigunguName = sigunguName;
            this.dongName = dongName;
            this.sectionTitle = sectionTitle;
            this.content = content;
        }

        private String sigunguName() {
            return sigunguName;
        }

        private String dongName() {
            return dongName;
        }

        private String sectionTitle() {
            return sectionTitle;
        }

        private String content() {
            return content;
        }

        private float[] embedding() {
            return embedding;
        }

        private void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        private String embeddingText() {
            return sigunguName + " " + dongName + " " + sectionTitle + " " + content;
        }
    }
}
