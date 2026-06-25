package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 동네 추천 상세 화면의 "AI가 분석한 동네 요약" 영역에 바로 표시할 응답이다.
 *
 * <p>프론트가 마크다운을 해석하지 않아도 되도록 대표 문장, 핵심 bullet, 주의 문구를
 * 분리해서 내려준다. 이후 실제 LLM 요약으로 확장하더라도 화면 계약은 유지할 수 있다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodSummaryResponse {

    /** 카드 상단에 보여줄 한 문장 요약이다. */
    private String lead;

    /** 사용자가 빠르게 읽을 수 있는 핵심 해석 목록이다. */
    private List<String> points = new ArrayList<>();

    /** RAG 문서를 실제로 찾아 참고했는지 여부다. */
    private boolean ragDocumentFound;

    /** 점수와 공개 데이터 해석 시 주의할 점이다. */
    private String caution;
}
