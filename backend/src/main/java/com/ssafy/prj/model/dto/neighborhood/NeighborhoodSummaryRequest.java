package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 동네 추천 상세 화면에서 AI/RAG 요약을 만들 때 프론트가 전달하는 데이터다.
 *
 * <p>1차 구현에서는 프론트가 이미 가진 추천 결과를 다시 계산하지 않고 그대로 보낸다.
 * 백엔드는 이 값에 RAG 문서 검색 결과를 덧붙여 사용자가 이해하기 쉬운 요약 문장을 만든다.
 * 이렇게 하면 추천 API의 점수 계산 로직은 건드리지 않으면서 상세 화면의 설명만 독립적으로 개선할 수 있다.</p>
 */
@Data
public class NeighborhoodSummaryRequest {

    /** 요약 대상 동네다. 법정동명, 자치구명, 카테고리별 점수가 포함된다. */
    private RecommendedNeighborhood neighborhood;

    /** 사용자가 선택한 거래 유형이다. 예: 매매, 전세, 월세 */
    private String dealType;

    /** 화면에 보여주는 예산 조건 문구다. 예: 보증금 5억원 이하, 월세 200만원 이하 */
    private String budgetLabel;

    /** 상세 화면에서 조회된 조건 일치 매물 수다. */
    private int houseCount;

    /** 사용자가 선택한 생활 우선순위 코드 목록이다. 예: TRANSPORT, SHOPPING, PET */
    private List<String> priorities = new ArrayList<>();
}
