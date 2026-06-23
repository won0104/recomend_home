package com.ssafy.prj.model.service.neighborhood;

import java.util.List;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

/**
 * 사용자 우선순위에 따라 후보 동네를 추천한다.
 */
public interface NeighborhoodRecommendationService {

    List<RecommendedNeighborhood> recommend(
        List<String> dongCodes,
        List<SurveyCategory> priorities
    );
}
