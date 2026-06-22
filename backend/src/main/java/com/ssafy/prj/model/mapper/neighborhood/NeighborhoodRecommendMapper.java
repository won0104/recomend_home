package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NeighborhoodRecommendMapper {

    List<String> findLegalDongCodes(@Param("originLat") double originLat,
            @Param("originLng") double originLng,
            @Param("radiusKm") double radiusKm,
            @Param("maxPrice") Integer maxPrice);
}
