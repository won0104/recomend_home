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
import com.ssafy.prj.model.dto.Board;
import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.service.BoardService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "게시판")
public class BoardApiController {

    private final BoardService boardService;

    @GetMapping
    @Operation(summary = "게시글 목록", description = "공개 API입니다. keyword로 제목이나 내용을 검색합니다.")
    public ResponseEntity<?> list(@Parameter(description = "검색어", example = "강남") @RequestParam(required = false) String keyword) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("게시글 목록입니다.", boardService.findAll(keyword)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세", description = "공개 API입니다. 조회수가 증가합니다.")
    public ResponseEntity<?> detail(@Parameter(description = "게시글 ID", example = "1") @PathVariable int id) throws Exception {
        Board board = boardService.findByIdAndIncreaseView(id);
        if (board == null) throw new ApiNotFoundException("게시글을 찾을 수 없습니다.");
        return ResponseEntity.ok(ApiResponse.ok("게시글 상세입니다.", board));
    }

    @GetMapping("/me")
    @Operation(summary = "내가 쓴 게시글 목록", description = "로그인이 필요합니다. 현재 사용자가 작성한 글을 반환합니다.")
    public ResponseEntity<?> my(@AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        return ResponseEntity.ok(ApiResponse.ok("내가 쓴 글 목록입니다.",
                boardService.findByAuthorUserId(user.getUserId())));
    }

    @PostMapping
    @Operation(summary = "게시글 작성", description = "로그인이 필요합니다. 작성자는 현재 Spring Security 사용자로 저장됩니다.")
    public ResponseEntity<?> register(@RequestBody BoardRequest request,
                                      @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        Board saved = boardService.register(request.title(), request.content(), user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("게시글이 등록되었습니다.", saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "게시글 수정", description = "로그인이 필요합니다. 서비스 로직에 따라 작성자나 관리자가 수정할 수 있습니다.")
    public ResponseEntity<?> update(@Parameter(description = "게시글 ID", example = "1") @PathVariable int id,
                                    @RequestBody BoardRequest request,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        Board updated = boardService.update(id, request.title(), request.content(), user.getUserId(), user.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("게시글이 수정되었습니다.", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제", description = "로그인이 필요합니다. 서비스 로직에 따라 작성자나 관리자가 삭제할 수 있습니다.")
    public ResponseEntity<?> delete(@Parameter(description = "게시글 ID", example = "1") @PathVariable int id,
                                    @AuthenticationPrincipal CustomUserDetails details) throws Exception {
        UserAccount user = ApiControllerSupport.requireLogin(details);
        boardService.delete(id, user.getUserId(), user.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("게시글이 삭제되었습니다.", null));
    }

    public record BoardRequest(
            @Schema(description = "게시글 제목", example = "강남구 생활권 정보 공유") String title,
            @Schema(description = "게시글 내용", example = "역세권과 상권이 괜찮은 지역입니다.") String content) {}
}
