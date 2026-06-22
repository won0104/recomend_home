package com.ssafy.prj.model.dto.neighborhood;

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
}
