package com.ssafy.prj.chat.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.prj.model.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat/rag")
@RequiredArgsConstructor
public class NeighborhoodProfileRagController {

    private final NeighborhoodProfileEmbeddingService embeddingService;
    private final NeighborhoodProfileFileIngestService fileIngestService;

    @PostMapping("/profiles")
    public ResponseEntity<?> ingestProfiles(@RequestBody NeighborhoodProfileIngestRequest request) {
        int count = embeddingService.ingest(request == null ? null : request.documents());
        return ResponseEntity.ok(ApiResponse.ok("동네 성향 자료 embedding 저장 완료", count));
    }

    @PostMapping("/profiles/reload-generated")
    public ResponseEntity<?> reloadGeneratedProfiles() {
        NeighborhoodProfileFileIngestService.GeneratedProfileIngestResult result =
            fileIngestService.ingestGeneratedProfiles();
        return ResponseEntity.ok(ApiResponse.ok("generated RAG 문서 embedding 저장 완료", result));
    }
}
