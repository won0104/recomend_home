package com.ssafy.prj.model.dto.neighborhood;

import lombok.Data;

/** 법정동별 버스 정류장 수 조회 결과를 담는다. */
@Data
public class NeighborhoodBusStopCount {
    private String dongCode;
    private int busStopCount;
}
