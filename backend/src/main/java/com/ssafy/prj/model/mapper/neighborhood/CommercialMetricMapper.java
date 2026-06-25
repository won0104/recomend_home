package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;

@Mapper
public interface CommercialMetricMapper {

    List<NeighborhoodCommercialCount> countByDongCodes(
        @Param("dongCodes") List<String> dongCodes,
        @Param("category") String category
    );

    List<NeighborhoodCommercialCount> countAllSeoulDongCodesByCategory(
        @Param("category") String category
    );
}
