package com.ssafy.prj.chat.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.domain.NeighborhoodChatSession;

@Service
public class NeighborhoodPreferenceMemoryService {
    private final VectorStore vectorStore;

    public NeighborhoodPreferenceMemoryService(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStore = vectorStoreProvider.getIfAvailable();
    }

    public void rememberCompletedRecommendation(NeighborhoodChatSession session) {
        if (vectorStore == null || session == null) {
            return;
        }
        try {
            String text = """
                    사용자는 %s 기준 %.1fkm 반경, 예산 %s, 생활환경 우선순위 %s 조건으로 동네추천을 요청했다.
                    """.formatted(
                    session.getBasePlaceName(),
                    session.getRadiusKm(),
                    session.getMaxPrice() == null ? "상관없음" : session.getMaxPrice() + "만원 이하",
                    String.join(" > ", session.getPriorities()));

            vectorStore.add(java.util.List.of(new Document(
                    "preference-" + UUID.randomUUID(),
                    text,
                    Map.of(
                            "domain", "neighborhood-chat",
                            "type", "USER_PREFERENCE",
                            "sessionId", session.getSessionId(),
                            "createdAt", Instant.now().toString()))));
        } catch (Exception ignored) {
        }
    }
}
