package com.ssafy.prj.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.domain.NeighborhoodChatHistoryMessage;
import com.ssafy.prj.chat.domain.NeighborhoodChatSession;

@Service
public class NeighborhoodQuestionPromptService {
    private static final int HISTORY_LIMIT = 8;

    private final org.springframework.ai.chat.client.ChatClient chatClient;

    public NeighborhoodQuestionPromptService(
            ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> chatClientBuilderProvider) {
        org.springframework.ai.chat.client.ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
    }

    public String openingQuestion(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history) {
        return ask(session, history, "ASK_PLACE",
                "기준 위치를 받아야 한다. 사용자가 장소명, 건물명, 역명 등을 자연스럽게 말하게 유도한다.",
                """
                안녕하세요. 동네추천 챗봇이에요.

                먼저 기준 위치를 알려주세요.
                예: 삼성역, 서울시청, 부산역, 회사명, 건물명
                """);
    }

    public String askRadius(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history, String prefix) {
        return ask(session, history, "ASK_RADIUS",
                "기준 위치가 정해졌으니 반경을 받아야 한다. km 단위, 도보권, 차로 몇 분 같은 표현도 자연스럽게 받아준다.",
                prefix + """

                어느 정도 반경까지 볼까요?

                예: 1km, 3km, 5km, 도보권, 차로 20분
                """);
    }

    public String askBudget(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history, String prefix) {
        return ask(session, history, "ASK_BUDGET",
                "예산 정보를 받아야 한다. 월세, 전세, 상한금액, 상관없음 같은 표현을 자연스럽게 유도한다.",
                prefix + """

                예산은 어느 정도로 볼까요?

                예: 8천만원 이하, 1억 이하, 상관없음
                """);
    }

    public String askLifestyle(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history, String prefix) {
        return ask(session, history, "ASK_LIFESTYLE",
                "생활환경 우선순위를 최대 3개까지 받아야 한다. 사용자가 우선순위와 설명을 섞어서 말해도 자연스럽게 이해하도록 유도한다.",
                prefix + """

                중요하게 보는 생활환경을 최대 3개까지 알려주세요.

                예: 공원 > 카페/외식 > 의료
                """);
    }

    public String confirmLifestyle(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history, String priorityLines) {
        return ask(session, history, "CONFIRM_LIFESTYLE",
                "사용자가 말한 생활환경 우선순위를 다시 보여주고 확인을 받는다. 틀렸으면 바로 수정할 수 있게 안내한다.",
                """
                이렇게 이해했어요.

                %s

                맞으면 '네', 틀리면 원하는 항목을 바로 수정해서 말해주세요.
                """.formatted(priorityLines));
    }

    public String askCorrectionTarget(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history) {
        return ask(session, history, "ASK_CORRECTION_TARGET",
                "사용자가 수정을 원하지만 어떤 항목인지 불명확하다. 기준 위치, 반경, 예산, 생활환경 중 하나를 고르게 한다.",
                """
                어떤 항목을 다시 바꿀까요?

                - 기준 위치
                - 반경
                - 예산
                - 생활환경
                """);
    }

    private String ask(NeighborhoodChatSession session, List<NeighborhoodChatHistoryMessage> history,
            String step, String task, String fallback) {
        if (chatClient == null) {
            return fallback;
        }
        try {
            String content = chatClient.prompt()
                    .system("""
                            너는 동네추천을 위한 대화형 안내 챗봇이다.

                            규칙:
                            - 한 번에 질문은 하나만 한다.
                            - 사용자의 말투를 과하게 따라하지 말고, 짧고 자연스럽게 말한다.
                            - 이미 확정된 값은 다시 묻지 않는다.
                            - history에 같은 질문이 있으면 다른 표현으로만 한 번 더 묻는다.
                            - 내부 상태나 step 이름은 절대 노출하지 않는다.
                            - 필요하면 친절한 이모지 1개 정도만 가볍게 사용한다.
                            - 출력은 markdown이 아닌 일반 문장만 사용한다.
                            """)
                    .user("""
                            현재 step: %s

                            지금 해야 할 일:
                            %s

                            현재 확정된 값:
                            - 기준 위치: %s
                            - 위도: %s
                            - 경도: %s
                            - 반경: %s
                            - 예산: %s
                            - 생활환경 우선순위: %s

                            최근 대화 history:
                            %s

                            사용자에게 보여줄 다음 질문만 한국어로 작성해줘.
                            질문은 1~3문장으로 짧게 유지해줘.
                            """.formatted(
                            step,
                            task,
                            value(session.getBasePlaceName()),
                            value(session.getLatitude()),
                            value(session.getLongitude()),
                            value(session.getRadiusKm()),
                            value(session.getMaxPrice()),
                            priorities(session.getPriorities()),
                            historySummary(history)))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return fallback;
            }
            return content.trim();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String historySummary(List<NeighborhoodChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "- 없음";
        }
        return history.stream()
                .skip(Math.max(0, history.size() - HISTORY_LIMIT))
                .map(entry -> "- %s: %s".formatted(value(entry.getRole()), value(entry.getContent())))
                .collect(Collectors.joining("\n"));
    }

    private String priorities(List<String> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            return "없음";
        }
        return String.join(" > ", priorities);
    }

    private String value(Object value) {
        return value == null ? "없음" : String.valueOf(value);
    }
}
