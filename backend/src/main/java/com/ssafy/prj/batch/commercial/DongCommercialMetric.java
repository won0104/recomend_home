package com.ssafy.prj.batch.commercial;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DongCommercialMetric {

    private final String dongCode;
    private final String category;
    private final int businessCount;
    private final LocalDate sourceDate;
}
