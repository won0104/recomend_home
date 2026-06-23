package com.ssafy.prj.chat.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class NeighborhoodRagService {
    private final org.springframework.ai.chat.client.ChatClient chatClient;
    private final VectorStore vectorStore;

    public NeighborhoodRagService(
            ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> chatClientBuilderProvider,
            ObjectProvider<VectorStore> vectorStoreProvider) {
        org.springframework.ai.chat.client.ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.vectorStore = vectorStoreProvider.getIfAvailable();
    }

    public String answer(String question) {
        if (question == null || question.isBlank()) {
            return "궁금한 내용을 한 문장으로 물어봐 주세요.";
        }
        if (chatClient == null || vectorStore == null) {
            return fallbackAnswer(question);
        }
        try {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(4)
                    .similarityThreshold(0.55)
                    .filterExpression("domain == 'neighborhood-chat'")
                    .build());
            String context = documents.stream()
                    .map(Document::getText)
                    .reduce("", (left, right) -> left + "\n---\n" + right);
            if (context.isBlank()) {
                return fallbackAnswer(question);
            }
            return chatClient.prompt()
                    .system("""
                            You answer questions about the neighborhood recommendation chatbot.
                            Answer in Korean. Use only the provided context.
                            Keep the answer concise, then guide the user back to the recommendation flow.
                            """)
                    .user("질문: " + question + "\n\n문서:\n" + context)
                    .call()
                    .content();
        } catch (Exception e) {
            return fallbackAnswer(question);
        }
    }

    private String fallbackAnswer(String question) {
        if (question.contains("반려") || question.contains("강아지") || question.contains("펫")) {
            return "반려동물은 반려동물과 함께 생활하기 좋은 환경을 뜻합니다. 산책, 동물병원 접근성, 생활 편의를 함께 고려할 수 있습니다.";
        }
        if (question.contains("점수") || question.contains("우선순위")) {
            return "생활환경 점수는 선택한 순서대로 반영합니다. 1순위는 가장 크게, 2순위와 3순위는 그보다 낮은 가중치로 반영합니다.";
        }
        if (question.contains("예산")) {
            return "예산은 매매가 상한으로 입력합니다. 예를 들어 8억 이하는 내부적으로 80000만원으로 변환합니다.";
        }
        return "동네추천 챗봇은 기준 위치, 반경, 예산, 생활환경 우선순위를 받아 추천 후보를 계산합니다.";
    }
}
