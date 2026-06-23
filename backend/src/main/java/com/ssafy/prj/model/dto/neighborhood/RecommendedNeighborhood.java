package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 동네 추천 결과 한 건을 표현한다.
 */
@Data
public class RecommendedNeighborhood {
    private int rank;
    private String lawdCd;
    private String sidoName;
    private String sigunguName;
    private String dongName;
    private Double score;
    /** 사용자가 선택한 순서대로 카테고리별 점수 내역을 담는다. */
    private List<CategoryScore> categoryScores = new ArrayList<>();
}
