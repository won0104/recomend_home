package com.ssafy.prj.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.service.FavoriteAreaService;
import com.ssafy.prj.model.service.UserAccountService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "회원")
public class MemberApiController {

    private final UserAccountService userAccountService;
    private final FavoriteAreaService favoriteAreaService;

    @GetMapping("/members/me")
    @Operation(summary = "내 회원 정보 조회", description = "로그인이 필요합니다. 현재 사용자의 회원 정보를 비밀번호 해시 없이 반환합니다.")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount loginUser = ApiControllerSupport.requireLogin(details);
        UserAccount user = userAccountService.findByUserId(loginUser.getUserId());
        if (user == null) throw new ApiNotFoundException("사용자를 찾을 수 없습니다.");
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", ApiControllerSupport.sanitize(user)));
    }

    @PostMapping("/members")
    @Operation(summary = "회원가입", description = "회원을 생성합니다. 비밀번호는 현재 SHA-256 호환 PasswordEncoder로 저장됩니다.")
    public ResponseEntity<?> register(@RequestBody MemberRegisterRequest request) throws Exception {
        UserAccount user = userAccountService.register(request.userId(), request.name(), request.email(),
                request.phone(), request.password());
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다.", ApiControllerSupport.sanitize(user)));
    }

    @PutMapping("/members/me")
    @Operation(summary = "내 회원 정보 수정", description = "로그인이 필요합니다. 비밀번호를 비우면 서비스 로직에 따라 기존 비밀번호가 유지됩니다.")
    public ResponseEntity<?> update(@RequestBody MemberUpdateRequest request,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount loginUser = ApiControllerSupport.requireLogin(details);

        UserAccount user = userAccountService.update(loginUser.getUserId(), request.name(), request.email(),
                request.phone(), request.password());
        copyUser(details.getUser(), user);
        return ResponseEntity.ok(ApiResponse.ok("회원 정보가 수정되었습니다.", ApiControllerSupport.sanitize(user)));
    }

    @DeleteMapping("/members/me")
    @Operation(summary = "내 계정 탈퇴", description = "로그인이 필요합니다. 관심지역을 먼저 삭제한 뒤 회원을 삭제하고 보안 컨텍스트를 초기화합니다.")
    public ResponseEntity<?> delete(@AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount loginUser = ApiControllerSupport.requireLogin(details);

        String userId = loginUser.getUserId();
        favoriteAreaService.deleteAllByUserId(userId);
        boolean deleted = userAccountService.delete(userId);
        if (!deleted) throw new ApiNotFoundException("사용자를 찾을 수 없습니다.");
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴가 완료되었습니다.", null));
    }

    @GetMapping("/admin/members")
    @Operation(summary = "관리자 회원 목록", description = "관리자 로그인이 필요합니다. 비밀번호 필드를 제거한 전체 회원 목록을 반환합니다.")
    public ResponseEntity<?> members(@AuthenticationPrincipal CustomUserDetails details) throws Exception {
        ApiControllerSupport.requireAdmin(details);

        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        for (UserAccount user : userAccountService.findAll()) {
            users.add(ApiControllerSupport.sanitize(user));
        }
        return ResponseEntity.ok(ApiResponse.ok("회원 목록을 조회했습니다.", users));
    }

    @PatchMapping("/admin/members/{userId}/role")
    @Operation(summary = "회원 권한 변경", description = "관리자 로그인이 필요합니다. role 값으로 USER 또는 ADMIN을 사용합니다.")
    public ResponseEntity<?> changeRole(@Parameter(description = "대상 사용자 ID", example = "ssafy") @PathVariable String userId,
                                        @RequestBody RoleChangeRequest request,
                                        @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount admin = ApiControllerSupport.requireAdmin(details);

        UserAccount updated = userAccountService.changeRole(admin.getUserId(), userId, request.role());
        return ResponseEntity.ok(ApiResponse.ok("권한이 변경되었습니다.", ApiControllerSupport.sanitize(updated)));
    }

    private void copyUser(UserAccount target, UserAccount source) {
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setPasswordHash(source.getPasswordHash());
        target.setRole(source.getRole());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    public record MemberRegisterRequest(
            @Schema(description = "로그인 ID", example = "user01") String userId,
            @Schema(description = "회원 이름", example = "Kim SSAFY") String name,
            @Schema(description = "이메일", example = "user01@ssafy.com") String email,
            @Schema(description = "전화번호", example = "010-1234-5678") String phone,
            @Schema(description = "비밀번호 원문", example = "1234") String password) {}

    public record MemberUpdateRequest(
            @Schema(description = "회원 이름", example = "Kim SSAFY") String name,
            @Schema(description = "이메일", example = "user01@ssafy.com") String email,
            @Schema(description = "전화번호", example = "010-1234-5678") String phone,
            @Schema(description = "새 비밀번호 원문", example = "1234") String password) {}

    public record RoleChangeRequest(
            @Schema(description = "부여할 권한", example = "ADMIN", allowableValues = {"USER", "ADMIN"}) String role) {}
}
