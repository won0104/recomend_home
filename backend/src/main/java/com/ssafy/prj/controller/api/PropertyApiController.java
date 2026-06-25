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
    @Operation(summary = "매매·전월세 실거래 검색", description = "공개 API입니다. contractType은 deal(매매), rent(전월세), all(전체)이며, rentType은 all·jeonse·monthly를 사용합니다. 금액은 모두 만원 단위입니다.")
    public ResponseEntity<?> search(@Parameter(description = "주택 유형", example = "APT") @RequestParam(required = false) String housingType,
                                    @Parameter(description = "거래 유형: deal, rent, all", example = "rent") @RequestParam(required = false) String contractType,
                                    @Parameter(description = "전월세 유형: all, jeonse, monthly", example = "monthly") @RequestParam(required = false) String rentType,
                                    @Parameter(description = "거래 년월 또는 일자", example = "202401") @RequestParam(required = false) String dealYmd,
                                    @Parameter(description = "법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                    @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                    @Parameter(description = "아파트명 또는 주소 검색어", example = "래미안") @RequestParam(required = false) String keyword,
                                    @Parameter(description = "매매가 최소(만원)", example = "50000") @RequestParam(required = false) Integer dealAmountMin,
                                    @Parameter(description = "매매가 최대(만원)", example = "100000") @RequestParam(required = false) Integer dealAmountMax,
                                    @Parameter(description = "보증금 최소(만원)", example = "1000") @RequestParam(required = false) Integer depositMin,
                                    @Parameter(description = "보증금 최대(만원)", example = "5000") @RequestParam(required = false) Integer depositMax,
                                    @Parameter(description = "월세 최소(만원)", example = "50") @RequestParam(required = false) Integer monthlyRentMin,
                                    @Parameter(description = "월세 최대(만원)", example = "150") @RequestParam(required = false) Integer monthlyRentMax) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("거래 검색 결과입니다.",
                realEstateService.search(condition(housingType, contractType, rentType, dealYmd, lawdCd, dong, keyword,
                        dealAmountMin, dealAmountMax, depositMin, depositMax, monthlyRentMin, monthlyRentMax))));
    }

    @GetMapping("/summary/total")
    @Operation(summary = "DB 전체 거래 건수 요약", description = "필터 없이 DB에 적재된 전체 매매·전월세 건수와 평균 매매가를 반환합니다. 홈 화면 초기 통계 카드용입니다.")
    public ResponseEntity<?> totalSummary() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("전체 거래 통계입니다.", realEstateService.summarizeAll()));
    }

    @GetMapping("/summary")
    @Operation(summary = "매매·전월세 실거래 요약", description = "공개 API입니다. 검색과 같은 거래·보증금·월세 조건으로 전체·매매·전월세 건수를 집계하고, 평균 매매가는 매매만 대상으로 반환합니다.")
    public ResponseEntity<?> summary(@Parameter(description = "주택 유형", example = "APT") @RequestParam(required = false) String housingType,
                                     @Parameter(description = "거래 유형: deal, rent, all", example = "rent") @RequestParam(required = false) String contractType,
                                     @Parameter(description = "전월세 유형: all, jeonse, monthly", example = "monthly") @RequestParam(required = false) String rentType,
                                     @Parameter(description = "거래 년월 또는 일자", example = "202401") @RequestParam(required = false) String dealYmd,
                                     @Parameter(description = "법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                     @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                     @Parameter(description = "아파트명 또는 주소 검색어", example = "래미안") @RequestParam(required = false) String keyword,
                                     @Parameter(description = "매매가 최소(만원)", example = "50000") @RequestParam(required = false) Integer dealAmountMin,
                                     @Parameter(description = "매매가 최대(만원)", example = "100000") @RequestParam(required = false) Integer dealAmountMax,
                                     @Parameter(description = "보증금 최소(만원)", example = "1000") @RequestParam(required = false) Integer depositMin,
                                     @Parameter(description = "보증금 최대(만원)", example = "5000") @RequestParam(required = false) Integer depositMax,
                                     @Parameter(description = "월세 최소(만원)", example = "50") @RequestParam(required = false) Integer monthlyRentMin,
                                     @Parameter(description = "월세 최대(만원)", example = "150") @RequestParam(required = false) Integer monthlyRentMax) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("거래 분석 결과입니다.",
                realEstateService.summarize(condition(housingType, contractType, rentType, dealYmd, lawdCd, dong, keyword,
                        dealAmountMin, dealAmountMax, depositMin, depositMax, monthlyRentMin, monthlyRentMax))));
    }

    private SearchCondition condition(String housingType, String contractType, String rentType, String dealYmd,
                                      String lawdCd, String dong, String keyword,
                                      Integer dealAmountMin, Integer dealAmountMax,
                                      Integer depositMin, Integer depositMax,
                                      Integer monthlyRentMin, Integer monthlyRentMax) {
        SearchCondition c = new SearchCondition();
        c.setHousingType(housingType);
        c.setContractType(contractType);
        c.setRentType(rentType);
        c.setDealYmd(dealYmd);
        c.setLawdCd(lawdCd);
        c.setDong(dong);
        c.setKeyword(keyword);
        c.setDealAmountMin(dealAmountMin);
        c.setDealAmountMax(dealAmountMax);
        c.setDepositMin(depositMin);
        c.setDepositMax(depositMax);
        c.setMonthlyRentMin(monthlyRentMin);
        c.setMonthlyRentMax(monthlyRentMax);
        return c;
    }
}
