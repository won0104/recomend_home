package com.ssafy.prj.controller.api.neighborhood;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSummaryRequest;
import com.ssafy.prj.model.service.neighborhood.NeighborhoodRecommendService;
import com.ssafy.prj.model.service.neighborhood.NeighborhoodSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/neighborhoods")
@RequiredArgsConstructor
@Tag(name = "동네 추천")
public class NeighborhoodRecommendApiController {

    private final NeighborhoodRecommendService neighborhoodRecommendService;
    private final NeighborhoodSummaryService neighborhoodSummaryService;

    @PostMapping("/recommend")
    @Operation(summary = "맞춤 동네 추천 조건 필터링", description = "기준 위치 반경과 거래 유형별 최대 예산으로 후보 법정동을 필터링합니다. 매매는 dealAmountMax, 전세·월세는 depositMax, 월세는 monthlyRentMax를 만원 단위로 사용합니다.")
    public ResponseEntity<?> recommend(@RequestBody NeighborhoodRecommendRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("동네 추천 후보입니다.",
                neighborhoodRecommendService.recommend(request)));
    }

    @PostMapping("/summary")
    @Operation(summary = "추천 동네 AI/RAG 요약", description = "추천 상세 화면에서 선택한 동네의 점수와 RAG 문서 근거를 조합해 생활환경 요약을 반환합니다.")
    public ResponseEntity<?> summarize(@RequestBody NeighborhoodSummaryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("추천 동네 요약입니다.",
                neighborhoodSummaryService.summarize(request)));
    }
}
