package com.ssafy.prj.chat.service;

public record RagSearchResult(
    String sigunguName,
    String dongName,
    String sectionTitle,
    String content,
    double score,
    boolean embeddingBased
) {
}
