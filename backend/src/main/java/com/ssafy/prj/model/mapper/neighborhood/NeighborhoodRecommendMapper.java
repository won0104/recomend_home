package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

@Mapper
public interface NeighborhoodRecommendMapper {

    /**
     * 출발 위치·반경·예산 조건으로 점수 계산 대상이 될 법정동 코드를 찾는다.
     * 이 단계는 "후보 추리기"만 담당하며, 생활 우선순위 점수 계산은 Service가 이어서 수행한다.
     */
    List<String> findLegalDongCodes(@Param("request") NeighborhoodRecommendRequest request);

    /**
     * 최종 추천 코드에 사람이 읽을 수 있는 시·군·구·동 이름을 붙인다.
     * 점수와 순위는 점수 계산 서비스가 만들고, 이 조회는 지역명만 보강한다.
     */
    List<RecommendedNeighborhood> findNeighborhoodsByDongCodes(
            @Param("dongCodes") List<String> dongCodes);
}
