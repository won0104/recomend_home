package com.ssafy.prj.batch.bus;

import java.math.BigDecimal;

/** 서울 버스 정류소 위치 API에서 읽은 정류소 식별자와 WGS84 좌표다. */
public record BusStopCoordinate(
    String stopsNo,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
