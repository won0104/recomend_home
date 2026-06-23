package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;

@Mapper
public interface ParkMapper {

    /** 후보 동별 법정동 코드가 매핑된 공원 수를 조회한다. */
    List<NeighborhoodParkCount> countByDongCodes(
        @Param("dongCodes") List<String> dongCodes
    );
}
