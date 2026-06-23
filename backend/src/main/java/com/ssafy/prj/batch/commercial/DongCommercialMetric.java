package com.ssafy.prj.batch.commercial;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 원본 업소 하나가 아니라, 배치가 압축한 "동 하나의 카테고리별 업소 수"를 표현한다.
 * 이 객체의 필드는 dong_commercial_metrics INSERT 컬럼과 일대일로 대응한다.
 */
@Getter
@AllArgsConstructor
public class DongCommercialMetric {

    private final String dongCode;
    private final String category;
    private final int businessCount;
    private final LocalDate sourceDate;
}
