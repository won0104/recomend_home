package com.ssafy.prj.model.service.neighborhood;

import java.util.List;
import java.util.Map;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

/**
 * 한 번의 추천 계산에서 나온 최종 순위와 카테고리별 중간 점수를 함께 담는다.
 *
 * <p>프론트 응답에는 보통 최종 순위만 필요하지만, 서버 로그에서는 "왜 이 동이 추천됐는지"를
 * 확인하기 위해 학교·공원·상권 등 각 카테고리의 점수도 필요하다. 중간 점수를 다시 계산하지 않고
 * 같은 계산 결과를 재사용하기 위해 이 record를 사용한다.</p>
 */
public record NeighborhoodRecommendationResult(
    List<RecommendedNeighborhood> recommendations,
    Map<SurveyCategory, Map<String, Double>> scoresByCategory,
    List<Double> priorityWeights
) {}
