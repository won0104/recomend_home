package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.service.HouseInfoService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "주택")
public class HouseInfoApiController {

    private static final int RECENT_DEALS_LIMIT = 20;

    private final HouseInfoService houseInfoService;

    @GetMapping("/houses")
    @Operation(summary = "주택 목록", description = "공개 API입니다. 법정동 코드, 동명, 아파트 검색어로 필터링합니다.")
    public ResponseEntity<?> list(@Parameter(description = "법정동 코드", example = "11680") @RequestParam(required = false) String lawdCd,
                                  @Parameter(description = "동명", example = "개포동") @RequestParam(required = false) String dong,
                                  @Parameter(description = "아파트 검색어", example = "래미안") @RequestParam(required = false) String keyword) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("주택 목록입니다.",
                houseInfoService.search(lawdCd, dong, keyword)));
    }

    @GetMapping("/houses/{aptSeq}")
    @Operation(summary = "주택 상세", description = "공개 API입니다. 아파트 식별값으로 주택 하나를 조회합니다.")
    public ResponseEntity<?> detail(@Parameter(description = "아파트 식별값", example = "11680-1001") @PathVariable String aptSeq) throws Exception {
        HouseInfo house = houseInfoService.findByAptSeq(aptSeq);
        if (house == null) throw new ApiNotFoundException("주택 정보를 찾을 수 없습니다.");
        return ResponseEntity.ok(ApiResponse.ok("주택 상세입니다.", house));
    }

    @GetMapping("/houses/{aptSeq}/deals")
    @Operation(summary = "최근 주택 거래 내역", description = "공개 API입니다. 한 아파트의 최근 거래 내역을 반환합니다.")
    public ResponseEntity<?> recentDeals(@Parameter(description = "아파트 식별값", example = "11680-1001") @PathVariable String aptSeq,
                                         @Parameter(description = "최대 조회 건수", example = "20") @RequestParam(required = false) Integer limit) throws Exception {
        int resolvedLimit = limit == null ? RECENT_DEALS_LIMIT : limit;
        return ResponseEntity.ok(ApiResponse.ok("최근 거래 내역입니다.",
                houseInfoService.findRecentDeals(aptSeq, resolvedLimit)));
    }

    @PutMapping("/admin/houses/{aptSeq}")
    @Operation(summary = "주택 정보 수정", description = "관리자 로그인이 필요합니다. 수정 가능한 주택 필드를 변경합니다.")
    public ResponseEntity<?> update(@Parameter(description = "아파트 식별값", example = "11680-1001") @PathVariable String aptSeq,
                                    @RequestBody HouseUpdateRequest request,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        ApiControllerSupport.requireAdmin(details);
        HouseInfo updated = houseInfoService.update(aptSeq, request.aptNm(), request.buildYear(),
                request.latitude(), request.longitude());
        return ResponseEntity.ok(ApiResponse.ok("주택 정보가 수정되었습니다.", updated));
    }

    public record HouseUpdateRequest(
            @Schema(description = "아파트명", example = "래미안블레스티지") String aptNm,
            @Schema(description = "건축연도", example = "2019") Integer buildYear,
            @Schema(description = "위도", example = "37.4979") String latitude,
            @Schema(description = "경도", example = "127.0276") String longitude) {}
}
