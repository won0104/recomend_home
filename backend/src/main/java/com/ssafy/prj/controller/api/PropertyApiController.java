package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.SearchCondition;
import com.ssafy.prj.model.service.RealEstateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "부동산 거래")
public class PropertyApiController {

    private final RealEstateService realEstateService;

    @GetMapping("/search")
    @Operation(summary = "실거래 검색", description = "공개 API입니다. 모든 조회 파라미터는 선택값이며, 테스트할 때는 lawdCd 또는 dong부터 넣어보면 좋습니다.")
    public ResponseEntity<?> search(@Parameter(description = "주택 유형", example = "APT") @RequestParam(required = false) String housingType,
                                    @Parameter(description = "계약 유형", example = "SALE") @RequestParam(required = false) String contractType,
                                    @Parameter(description = "거래 년월 또는 일자", example = "202401") @RequestParam(required = false) String dealYmd,
                                    @Parameter(description = "법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                    @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                    @Parameter(description = "아파트명 또는 주소 검색어", example = "래미안") @RequestParam(required = false) String keyword) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("거래 검색 결과입니다.",
                realEstateService.search(condition(housingType, contractType, dealYmd, lawdCd, dong, keyword))));
    }

    @GetMapping("/summary")
    @Operation(summary = "실거래 요약", description = "공개 API입니다. 실거래 검색과 같은 필터를 사용해 요약 값을 반환합니다.")
    public ResponseEntity<?> summary(@Parameter(description = "주택 유형", example = "APT") @RequestParam(required = false) String housingType,
                                     @Parameter(description = "계약 유형", example = "SALE") @RequestParam(required = false) String contractType,
                                     @Parameter(description = "거래 년월 또는 일자", example = "202401") @RequestParam(required = false) String dealYmd,
                                     @Parameter(description = "법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                     @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                     @Parameter(description = "아파트명 또는 주소 검색어", example = "래미안") @RequestParam(required = false) String keyword) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("거래 분석 결과입니다.",
                realEstateService.summarize(condition(housingType, contractType, dealYmd, lawdCd, dong, keyword))));
    }

    private SearchCondition condition(String housingType, String contractType, String dealYmd,
                                      String lawdCd, String dong, String keyword) {
        SearchCondition c = new SearchCondition();
        c.setHousingType(housingType);
        c.setContractType(contractType);
        c.setDealYmd(dealYmd);
        c.setLawdCd(lawdCd);
        c.setDong(dong);
        c.setKeyword(keyword);
        return c;
    }
}
