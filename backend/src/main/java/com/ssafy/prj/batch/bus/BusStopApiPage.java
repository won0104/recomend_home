package com.ssafy.prj.batch.bus;

import java.util.List;

/** 서울 Open API 한 페이지의 전체 건수와 정류소 목록을 함께 전달한다. */
public record BusStopApiPage(
    int totalCount,
    List<BusStopCoordinate> stops
) {
}
