package com.ssafy.prj.model.service.neighborhood;

import java.util.List;

import com.ssafy.prj.model.service.neighborhood.dto.RecommendedNeighborhood;

/**
 * 법정동 기반 동네 추천 서비스 인터페이스
 */
public interface NeighborhoodRecommendationService {
    
    /**
     * 사용자 설문 응답을 기반으로 추천 동네 3개 반환
     * 
     * @param dongList 추천 후보 법정동 코드 목록
     * @param priorities 프론트엔드에서 전달한 우선순위 카테고리 3개
     * @return 추천 동네 3개 (상위부터 순서대로)
     */
    List<RecommendedNeighborhood> getRecommendations(
        List<String> dongList,
        List<SurveyCategory> priorities
    ) throws Exception;
}
