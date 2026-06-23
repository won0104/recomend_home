package com.ssafy.prj.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.chat.dto.NeighborhoodChatRequest;
import com.ssafy.prj.chat.service.NeighborhoodChatService;
import com.ssafy.prj.model.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat/neighborhood")
@RequiredArgsConstructor
public class NeighborhoodChatApiController {
    private final NeighborhoodChatService neighborhoodChatService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.ok("동네추천 챗봇을 시작합니다.",
                neighborhoodChatService.start(sessionId)));
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody NeighborhoodChatRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("동네추천 챗봇 응답입니다.",
                neighborhoodChatService.handle(request)));
    }
}
