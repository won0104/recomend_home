package com.ssafy.prj.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.chat.dto.ChatBotRequest;
import com.ssafy.prj.chat.dto.ChatBotResponse;
import com.ssafy.prj.chat.dto.ChatSessionResetRequest;
import com.ssafy.prj.chat.service.ChatBotService;
import com.ssafy.prj.chat.service.ChatStateService;
import com.ssafy.prj.model.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "챗봇")
public class ChatBotApiController {

    private final ChatBotService chatBotService;
    private final ChatStateService chatStateService;

    @PostMapping
    @Operation(summary = "LLM 기반 맞춤 동네 추천 챗봇")
    public ResponseEntity<?> chat(@RequestBody ChatBotRequest request) {
        String answer = chatBotService.chat(request);
        return ResponseEntity.ok(ApiResponse.ok("챗봇 응답입니다.", new ChatBotResponse(answer)));
    }

    @PostMapping("/reset")
    @Operation(summary = "챗봇 세션 초기화")
    public ResponseEntity<?> reset(@RequestBody ChatSessionResetRequest request) {
        if (request != null && request.sessionId() != null && !request.sessionId().isBlank()) {
            chatStateService.clearSession(request.sessionId());
        }
        return ResponseEntity.ok(ApiResponse.ok("챗봇 세션이 초기화되었습니다.", null));
    }
}
