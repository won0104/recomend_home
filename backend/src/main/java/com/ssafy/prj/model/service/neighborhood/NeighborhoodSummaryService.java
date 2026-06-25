package com.ssafy.prj.model.service.neighborhood;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSummaryRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSummaryResponse;

/** 동네 추천 상세 화면에 표시할 AI/RAG 기반 생활 요약을 만든다. */
public interface NeighborhoodSummaryService {

    NeighborhoodSummaryResponse summarize(NeighborhoodSummaryRequest request);
}
