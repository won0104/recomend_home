package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "JSON 요청으로 인증하고 성공하면 서버 세션을 생성합니다.")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.userId(), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("로그인했습니다.", ApiControllerSupport.sanitize(details.getUser())));
    }

    @GetMapping("/me")
    @Operation(
            summary = "현재 로그인 사용자 조회",
            description = "/api/auth/login으로 로그인한 뒤 실행합니다. 브라우저 세션이 유효하면 비밀번호가 제거된 회원 정보를 반환합니다.")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails details) {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        return ResponseEntity.ok(ApiResponse.ok("현재 로그인 사용자입니다.",
                ApiControllerSupport.sanitize(user)));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 보안 컨텍스트를 비우고 서버 세션을 만료합니다.")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        expireCookie(response, "JSESSIONID");
        expireCookie(response, "remember-me");
        return ResponseEntity.ok(ApiResponse.ok("로그아웃했습니다.", null));
    }

    private void expireCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public record LoginRequest(
            @Schema(description = "로그인 ID", example = "ssafy") String userId,
            @Schema(description = "비밀번호 원문", example = "1234") String password,
            @Schema(description = "로그인 상태 유지 여부", example = "false") Boolean rememberMe) {}
}
