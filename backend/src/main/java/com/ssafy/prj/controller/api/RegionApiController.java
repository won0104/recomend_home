package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.service.DongCodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
@Tag(name = "지역")
public class RegionApiController {

    private final DongCodeService dongCodeService;

    @GetMapping("/sido")
    @Operation(summary = "시도 목록", description = "공개 API입니다. 먼저 시도를 조회한 뒤 선택한 코드를 /api/regions/sigungu에 전달합니다.")
    public ResponseEntity<?> sido() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("시도 목록입니다.", dongCodeService.findSidoList()));
    }

    @GetMapping("/sigungu")
    @Operation(summary = "시군구 목록", description = "공개 API입니다. 보통 sidoCode를 전달합니다. parentCode와 admCode는 기존 화면 호환을 위해 허용합니다.")
    public ResponseEntity<?> sigungu(@Parameter(description = "시도 코드", example = "11") @RequestParam(required = false) String sidoCode,
                                     @Parameter(description = "대체 상위 코드", example = "11") @RequestParam(required = false) String sigunguCode,
                                     @Parameter(description = "대체 상위 코드", example = "11") @RequestParam(required = false) String parentCode,
                                     @Parameter(description = "대체 상위 코드", example = "11") @RequestParam(required = false) String admCode) throws Exception {
        String parent = firstNonBlank(sidoCode, sigunguCode, parentCode, admCode);
        return ResponseEntity.ok(ApiResponse.ok("시군구 목록입니다.", dongCodeService.findSigunguList(parent)));
    }

    @GetMapping("/emd")
    @Operation(summary = "읍면동 목록", description = "공개 API입니다. 보통 sigunguCode를 전달합니다. parentCode와 admCode는 기존 화면 호환을 위해 허용합니다.")
    public ResponseEntity<?> emd(@Parameter(description = "대체 상위 코드", example = "11680") @RequestParam(required = false) String sidoCode,
                                 @Parameter(description = "시군구 코드", example = "11680") @RequestParam(required = false) String sigunguCode,
                                 @Parameter(description = "대체 상위 코드", example = "11680") @RequestParam(required = false) String parentCode,
                                 @Parameter(description = "대체 상위 코드", example = "11680") @RequestParam(required = false) String admCode) throws Exception {
        String parent = firstNonBlank(sidoCode, sigunguCode, parentCode, admCode);
        return ResponseEntity.ok(ApiResponse.ok("읍면동 목록입니다.", dongCodeService.findEmdList(parent)));
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }
}
