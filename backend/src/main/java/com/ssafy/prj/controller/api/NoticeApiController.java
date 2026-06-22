package com.ssafy.prj.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.Notice;
import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.service.NoticeService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "공지사항")
public class NoticeApiController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "공지사항 목록", description = "공개 API입니다. keyword로 공지사항을 검색합니다.")
    public ResponseEntity<?> list(@Parameter(description = "검색어", example = "안내") @RequestParam(required = false) String keyword) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("공지사항 목록입니다.", noticeService.findAll(keyword)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "공지사항 상세", description = "공개 API입니다. 조회수가 증가합니다.")
    public ResponseEntity<?> detail(@Parameter(description = "공지사항 ID", example = "1") @PathVariable int id) throws Exception {
        Notice notice = noticeService.findByIdAndIncreaseView(id);
        if (notice == null) throw new ApiNotFoundException("공지사항을 찾을 수 없습니다.");
        return ResponseEntity.ok(ApiResponse.ok("공지사항 상세입니다.", notice));
    }

    @PostMapping
    @Operation(summary = "공지사항 작성", description = "관리자 로그인이 필요합니다.")
    public ResponseEntity<?> register(@RequestBody NoticeRequest request,
                                      @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount admin = ApiControllerSupport.requireAdmin(details);
        Notice saved = noticeService.register(request.title(), request.content(), admin.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 등록되었습니다.", saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "공지사항 수정", description = "관리자 로그인이 필요합니다.")
    public ResponseEntity<?> update(@Parameter(description = "공지사항 ID", example = "1") @PathVariable int id,
                                    @RequestBody NoticeRequest request,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        ApiControllerSupport.requireAdmin(details);
        Notice updated = noticeService.update(id, request.title(), request.content());
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 수정되었습니다.", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "공지사항 삭제", description = "관리자 로그인이 필요합니다.")
    public ResponseEntity<?> delete(@Parameter(description = "공지사항 ID", example = "1") @PathVariable int id,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        ApiControllerSupport.requireAdmin(details);
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 삭제되었습니다.", null));
    }

    public record NoticeRequest(
            @Schema(description = "공지사항 제목", example = "시스템 점검 안내") String title,
            @Schema(description = "공지사항 내용", example = "오늘 오후 10시부터 점검이 진행됩니다.") String content) {}
}
