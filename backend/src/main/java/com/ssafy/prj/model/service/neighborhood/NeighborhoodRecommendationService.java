package com.ssafy.prj.model.service.neighborhood;

import java.util.List;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

/**
 * 사용자 우선순위에 따라 후보 동네를 추천한다.
 */
public interface NeighborhoodRecommendationService {

    /**
     * 프론트 응답에 필요한 상위 추천 결과만 반환한다.
     * 카테고리별 중간 점수도 필요한 서버 로그 용도에는 recommendWithDetails를 사용한다.
     */
    List<RecommendedNeighborhood> recommend(
        List<String> dongCodes,
        List<SurveyCategory> priorities
    );

    /**
     * 최종 추천 순위와 각 우선순위 카테고리의 점수표를 함께 반환한다.
     * 동일한 점수 계산을 두 번 수행하지 않고 상세 로그를 만들기 위한 메서드다.
     */
    NeighborhoodRecommendationResult recommendWithDetails(
        List<String> dongCodes,
        List<SurveyCategory> priorities
    );
}
