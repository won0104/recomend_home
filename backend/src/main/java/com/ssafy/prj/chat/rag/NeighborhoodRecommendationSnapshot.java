package com.ssafy.prj.chat.rag;

import java.util.List;

public record NeighborhoodRecommendationSnapshot(
    List<RecommendedDongSnapshot> recommendations,
    List<String> priorities
) {
}
