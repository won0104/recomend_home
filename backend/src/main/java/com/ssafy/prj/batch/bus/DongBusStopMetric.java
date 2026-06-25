package com.ssafy.prj.batch.bus;

import java.time.LocalDate;

/** 추천에서 사용하는 법정동별 버스 정류소 수 스냅샷 한 행이다. */
public record DongBusStopMetric(
    String dongCode,
    int busStopCount,
    LocalDate sourceDate
) {
}
