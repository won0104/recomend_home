package com.ssafy.prj.chat.dto;

import java.util.List;

public record NeighborhoodExplanationSlot(
    Integer rank,
    String sidoName,
    String sigunguName,
    String dongName,
    List<String> focusKeywords,
    String question
) {
}
