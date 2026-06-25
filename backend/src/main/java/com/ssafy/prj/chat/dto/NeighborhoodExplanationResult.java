package com.ssafy.prj.chat.dto;

import java.util.List;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

public record NeighborhoodExplanationResult(
    RecommendedNeighborhood target,
    List<String> focusKeywords,
    String answer,
    boolean ragDocumentFound
) {
}
