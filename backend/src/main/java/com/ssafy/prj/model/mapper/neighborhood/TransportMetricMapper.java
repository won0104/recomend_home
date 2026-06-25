package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodBusStopCount;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSubwayCount;

/** 동네 추천 교통 점수에 필요한 버스·지하철 집계값을 조회한다. */
@Mapper
public interface TransportMetricMapper {

    List<NeighborhoodBusStopCount> countBusStopsByDongCodes(
        @Param("dongCodes") List<String> dongCodes
    );

    List<NeighborhoodBusStopCount> countAllSeoulBusStops();

    List<NeighborhoodSubwayCount> countSubwaysByDongCodes(
        @Param("dongCodes") List<String> dongCodes
    );

    List<NeighborhoodSubwayCount> countAllSeoulSubways();
}
