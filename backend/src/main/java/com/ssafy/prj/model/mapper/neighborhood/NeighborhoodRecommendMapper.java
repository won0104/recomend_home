package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse.LegalDongCandidate;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse.NeighborhoodSaleCandidate;

@Mapper
public interface NeighborhoodRecommendMapper {

    List<NeighborhoodSaleCandidate> findSaleCandidates(@Param("originLat") double originLat,
            @Param("originLng") double originLng,
            @Param("radiusKm") double radiusKm,
            @Param("maxPrice") Integer maxPrice,
            @Param("limit") int limit);

    List<LegalDongCandidate> findLegalDongCandidates(@Param("originLat") double originLat,
            @Param("originLng") double originLng,
            @Param("radiusKm") double radiusKm,
            @Param("maxPrice") Integer maxPrice);
}
