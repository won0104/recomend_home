package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.FavoriteArea;
import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.service.FavoriteAreaService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "관심지역")
public class FavoriteApiController {

    private final FavoriteAreaService favoriteAreaService;

    @GetMapping
    @Operation(summary = "관심지역 목록", description = "로그인이 필요합니다. 현재 사용자의 관심지역을 반환합니다.")
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        return ResponseEntity.ok(ApiResponse.ok("관심지역 목록입니다.",
                favoriteAreaService.findByUserId(user.getUserId())));
    }

    @PostMapping
    @Operation(summary = "관심지역 등록", description = "로그인이 필요합니다. sido-sigungu-emd 또는 lawdCd-dong 형식의 고유 key를 사용합니다.")
    public ResponseEntity<?> add(@RequestBody FavoriteRequest request,
                                 @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);

        FavoriteArea favorite = favoriteAreaService.add(user.getUserId(), request.key(), request.sido(),
                request.sigungu(), request.emd(), request.lawdCd());
        return ResponseEntity.ok(ApiResponse.ok("관심지역이 등록되었습니다.", favorite));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "관심지역 삭제", description = "로그인이 필요합니다. key로 관심지역 하나를 삭제합니다.")
    public ResponseEntity<?> delete(@Parameter(description = "관심지역 key", example = "11-680-103") @PathVariable String key,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);

        boolean deleted = favoriteAreaService.delete(user.getUserId(), key);
        if (!deleted) throw new ApiNotFoundException("관심지역을 찾을 수 없습니다.");
        return ResponseEntity.ok(ApiResponse.ok("관심지역이 삭제되었습니다.", null));
    }

    public record FavoriteRequest(
            @Schema(description = "화면에서 사용하는 관심지역 고유 key", example = "11-680-103") String key,
            @Schema(description = "시도명", example = "서울특별시") String sido,
            @Schema(description = "시군구명", example = "강남구") String sigungu,
            @Schema(description = "읍면동명", example = "개포동") String emd,
            @Schema(description = "법정동 코드", example = "11680") String lawdCd) {}
}
