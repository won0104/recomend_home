package com.ssafy.prj.chat.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.domain.NeighborhoodChatHistoryMessage;
import com.ssafy.prj.chat.domain.NeighborhoodChatSession;

@Service
public class NeighborhoodSlotPlannerService {
    private final org.springframework.ai.chat.client.ChatClient chatClient;

    public NeighborhoodSlotPlannerService(
            ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> chatClientBuilderProvider) {
        org.springframework.ai.chat.client.ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
    }

    public String openingQuestion(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history) {
        return plan(session, history, List.of("PLACE"),
                "첫 번째 slot은 기준 위치다. 사용자가 장소명, 건물명, 역명, 회사명 등을 자연스럽게 말하도록 유도한다.");
    }

    public String nextQuestion(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history,
            List<String> candidateSlots, String context) {
        return plan(session, history, candidateSlots, context);
    }

    private String plan(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history,
            List<String> candidateSlots, String context) {
        if (chatClient == null) {
            return fallback(candidateSlots, context);
        }
        try {
            String content = chatClient.prompt()
                    .system("""
                            너는 동네추천 챗봇의 slot planner다.
                            사용자의 응답과 최근 대화 history를 보고, 지금 가장 먼저 물어봐야 할 slot 1개를 고른다.

                            규칙:
                            - 질문은 하나만 한다.
                            - 이미 채워진 slot은 다시 묻지 않는다.
                            - candidateSlots 안에서만 nextSlot을 고른다.
                            - 출력은 JSON만 사용한다.
                            - 형식:
                              {"nextSlot":"RADIUS","question":"...","reason":"..."}
                            - nextSlot은 PLACE, RADIUS, BUDGET, LIFESTYLE, CONFIRM 중 하나다.
                            """)
                    .user("""
                            현재 확정된 값:
                            - 기준 위치: %s
                            - 위도: %s
                            - 경도: %s
                            - 반경: %s
                            - 예산: %s
                            - 생활환경 우선순위: %s

                            후보 slot:
                            %s

                            최근 history:
                            %s

                            상황 설명:
                            %s
                            """.formatted(
                            value(session.getBasePlaceName()),
                            value(session.getLatitude()),
                            value(session.getLongitude()),
                            value(session.getRadiusKm()),
                            value(session.getMaxPrice()),
                            value(session.getPriorities()),
                            String.join(", ", candidateSlots),
                            historySummary(history),
                            context))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return fallback(candidateSlots, context);
            }

            String question = extractQuestion(content);
            return question == null || question.isBlank() ? fallback(candidateSlots, context) : question.trim();
        } catch (Exception e) {
            return fallback(candidateSlots, context);
        }
    }

    private String fallback(List<String> candidateSlots, String context) {
        String slot = candidateSlots == null || candidateSlots.isEmpty() ? "RADIUS" : candidateSlots.get(0);
        return switch (slot == null ? "" : slot.toUpperCase(Locale.ROOT)) {
            case "PLACE" -> "기준 위치를 입력해주세요. 예: 삼성역, 부산역, 서울시청";
            case "RADIUS" -> context + "\n\n어느 정도 반경까지 볼까요? 예: 1km, 3km, 5km, 도보권, 차로 20분";
            case "BUDGET" -> context + "\n\n예산은 어느 정도로 볼까요? 예: 8천만원 이하, 1억 이하, 상관없음";
            case "LIFESTYLE" -> context + "\n\n중요하게 보는 생활환경을 최대 3개까지 알려주세요.";
            case "CONFIRM" -> context + "\n\n제가 이해한 내용이 맞는지 확인해 주세요.";
            default -> context + "\n\n다음 정보를 알려주세요.";
        };
    }

    private String extractQuestion(String content) {
        int questionIndex = content.indexOf("\"question\"");
        if (questionIndex < 0) {
            return null;
        }
        int colonIndex = content.indexOf(':', questionIndex);
        if (colonIndex < 0) {
            return null;
        }
        int startQuote = content.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return null;
        }
        int endQuote = content.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        return content.substring(startQuote + 1, endQuote);
    }

    private String historySummary(List<NeighborhoodChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "- 없음";
        }
        return history.stream()
                .map(entry -> "- %s: %s".formatted(value(entry.getRole()), value(entry.getContent())))
                .collect(Collectors.joining("\n"));
    }

    private String value(Object value) {
        return value == null ? "없음" : String.valueOf(value);
    }
}
