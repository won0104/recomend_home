package com.ssafy.prj.chat.dto;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;

public record ChatBotRecommendResult(
    String locationKeyword,
    Double latitude,
    Double longitude,
    Double radiusKm,
    Long maxPrice,
    String dealType,
    String housingType,
    NeighborhoodRecommendResponse recommendation
) {
}
