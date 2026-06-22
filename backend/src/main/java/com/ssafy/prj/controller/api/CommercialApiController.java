package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.CommercialSearchCondition;
import com.ssafy.prj.model.service.CommercialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commercials")
@RequiredArgsConstructor
@Tag(name = "상권")
public class CommercialApiController {

    private final CommercialService commercialService;

    @GetMapping
    @Operation(summary = "상권 점포 검색", description = "공개 API입니다. lawdCd, dong, 업종, 검색어로 필터링합니다. 테스트 응답을 줄이려면 limit을 설정합니다.")
    public ResponseEntity<?> search(@Parameter(description = "시군구 또는 법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                    @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                    @Parameter(description = "업종 대분류 코드", example = "Q") @RequestParam(required = false) String categoryL,
                                    @Parameter(description = "업종 중분류 코드", example = "Q01") @RequestParam(required = false) String categoryM,
                                    @Parameter(description = "점포명 또는 업종 검색어", example = "카페") @RequestParam(required = false) String keyword,
                                    @Parameter(description = "최대 조회 건수", example = "10") @RequestParam(required = false) Integer limit) throws Exception {
        CommercialSearchCondition c = new CommercialSearchCondition();
        c.setSigunguCode(lawdCd);
        c.setBeopjeongName(dong);
        c.setCategoryLCode(categoryL);
        c.setCategoryMCode(categoryM);
        c.setKeyword(keyword);
        c.setLimit(limit);
        return ResponseEntity.ok(ApiResponse.ok("상권 검색 결과입니다.", commercialService.search(c)));
    }

    @GetMapping("/categories")
    @Operation(summary = "상권 업종 대분류", description = "공개 API입니다. 이 결과의 업종 코드를 /api/commercials/subcategories의 categoryLCode로 사용합니다.")
    public ResponseEntity<?> categories() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("상권업종 대분류 목록입니다.",
                commercialService.findCategoryLargeList()));
    }

    @GetMapping("/subcategories")
    @Operation(summary = "상권 업종 중분류", description = "공개 API입니다. Q와 같은 대분류 코드를 전달합니다.")
    public ResponseEntity<?> subcategories(@Parameter(description = "업종 대분류 코드", example = "Q") @RequestParam(required = false) String categoryLCode) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("상권업종 중분류 목록입니다.",
                commercialService.findCategoryMediumList(categoryLCode)));
    }
}
