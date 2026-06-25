package com.ssafy.prj.model.dto.neighborhood;

import lombok.Data;

/**
 * 법정동별 지하철 접근성 집계 결과를 담는다.
 *
 * <p>{@code stationCount}는 물리적 역 이름 기준 개수, {@code lineCount}는 노선 다양성 개수다.
 * {@code subwayIndex}는 역 개수와 환승 노선 보너스를 함께 반영한 점수 계산용 원본 지표다.</p>
 */
@Data
public class NeighborhoodSubwayCount {
    private String dongCode;
    private int stationCount;
    private int lineCount;
    private int subwayIndex;
}
