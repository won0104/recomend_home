package com.ssafy.prj.chat.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class NeighborhoodProfileFileIngestService {

    private static final Pattern SIDO_PATTERN = Pattern.compile("\"sido_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GUGUN_PATTERN = Pattern.compile("\"gugun_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DONG_HEADING_PATTERN = Pattern.compile("(?m)^###\\s+(.+?)\\s*\\((\\d{10})\\)\\s*$");

    private final NeighborhoodProfileEmbeddingService embeddingService;
    private final ChatRagProperties properties;

    public NeighborhoodProfileFileIngestService(
        NeighborhoodProfileEmbeddingService embeddingService,
        ChatRagProperties properties
    ) {
        this.embeddingService = embeddingService;
        this.properties = properties;
    }

    public GeneratedProfileIngestResult ingestGeneratedProfiles() {
        Path directory = Path.of(properties.getGeneratedProfilePath()).toAbsolutePath().normalize();
        List<Path> markdownFiles = markdownFiles(directory);
        List<NeighborhoodProfileDocumentRequest> documents = new ArrayList<>();
        for (Path markdownFile : markdownFiles) {
            documents.addAll(parseMarkdownFile(markdownFile));
        }
        int ingestedCount = embeddingService.ingest(documents);
        return new GeneratedProfileIngestResult(directory.toString(), markdownFiles.size(), documents.size(), ingestedCount);
    }

    private List<Path> markdownFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("RAG 문서 폴더가 없습니다: " + directory);
        }
        try (var stream = Files.list(directory)) {
            return stream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("RAG 문서 폴더를 읽지 못했습니다: " + directory, exception);
        }
    }

    private List<NeighborhoodProfileDocumentRequest> parseMarkdownFile(Path markdownFile) {
        String markdown;
        try {
            markdown = Files.readString(markdownFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("RAG 문서 파일을 읽지 못했습니다: " + markdownFile, exception);
        }

        String sido = findFirst(SIDO_PATTERN, markdown, "서울특별시");
        String sigungu = findFirst(GUGUN_PATTERN, markdown, removeExtension(markdownFile.getFileName().toString()));
        List<NeighborhoodProfileDocumentRequest> documents = new ArrayList<>();
        addGugunSummaryDocument(documents, sido, sigungu, markdown);
        addDongDocuments(documents, sido, sigungu, markdown);
        return documents;
    }

    private void addGugunSummaryDocument(
        List<NeighborhoodProfileDocumentRequest> documents,
        String sido,
        String sigungu,
        String markdown
    ) {
        int firstDongHeadingIndex = firstDongHeadingIndex(markdown);
        if (firstDongHeadingIndex <= 0) {
            return;
        }
        String summary = markdown.substring(0, firstDongHeadingIndex).trim();
        if (!summary.isEmpty()) {
            documents.add(new NeighborhoodProfileDocumentRequest(
                sido,
                sigungu,
                null,
                null,
                "GUGUN_SUMMARY",
                summary
            ));
        }
    }

    private void addDongDocuments(
        List<NeighborhoodProfileDocumentRequest> documents,
        String sido,
        String sigungu,
        String markdown
    ) {
        Matcher matcher = DONG_HEADING_PATTERN.matcher(markdown);
        List<DongSection> sections = new ArrayList<>();
        while (matcher.find()) {
            sections.add(new DongSection(matcher.group(1).trim(), matcher.group(2), matcher.start()));
        }

        for (int index = 0; index < sections.size(); index++) {
            DongSection section = sections.get(index);
            int end = index + 1 < sections.size() ? sections.get(index + 1).startIndex() : markdown.length();
            String content = markdown.substring(section.startIndex(), end).trim();
            if (content.isEmpty()) {
                continue;
            }
            documents.add(new NeighborhoodProfileDocumentRequest(
                sido,
                sigungu,
                section.dongName(),
                section.lawdCd(),
                "DONG_PROFILE",
                content
            ));
        }
    }

    private int firstDongHeadingIndex(String markdown) {
        Matcher matcher = DONG_HEADING_PATTERN.matcher(markdown);
        return matcher.find() ? matcher.start() : -1;
    }

    private String findFirst(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return fallback;
    }

    private String removeExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? filename : filename.substring(0, dotIndex);
    }

    public record GeneratedProfileIngestResult(
        String directory,
        int fileCount,
        int parsedDocumentCount,
        int ingestedDocumentCount
    ) {
    }

    private record DongSection(
        String dongName,
        String lawdCd,
        int startIndex
    ) {
    }
}
