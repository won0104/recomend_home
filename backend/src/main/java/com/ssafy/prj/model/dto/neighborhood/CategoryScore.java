package com.ssafy.prj.model.dto.neighborhood;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 추천 결과 한 건에서 특정 카테고리의 점수 내역을 담는다. */
@Data
@AllArgsConstructor
public class CategoryScore {
    private String category;
    private double score;
    private double weight;
    private double contribution;
}
