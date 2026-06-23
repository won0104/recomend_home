package com.ssafy.prj.chat.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.dto.ChatBotRequest;
import com.ssafy.prj.chat.dto.ChatMessageDTO;

@Service
public class ChatBotService {

    private static final String SYSTEM_PROMPT = """
        너는 친절한 부동산 추천 전문가야.
        사용자가 원하는 주거 조건을 자연스럽게 대화로 수집해.
        반드시 아래 6가지 조건이 모두 모일 때까지 한 번에 하나씩 친절하게 질문해.
        1. 장소 키워드: 기준 위치 이름. 위도/경도가 아니라 서울역, 삼성역 같은 장소명으로 받아.
        2. 반경: km 단위 숫자.
        3. 예산: 만원 단위 숫자. 3억원은 30000으로 해석해.
        4. 거래유형: 매매, 전세, 월세 등.
        5. 주거형태: 아파트, 원룸, 오피스텔 등.
        6. 우선순위 3개: SCHOOL(학교), CAFE(카페/외식), CULTURE(운동/문화), SHOPPING(장보기/생활편의), PARK(공원/산책), MEDICAL(의료), PET(반려동물) 중 정확히 3개.

        조건이 모두 모이면 recommendNeighborhood 함수를 실행해.
        함수 결과를 받은 뒤에는 추천 Top3의 동네명, 강점, 조건 해석을 한국어로 간결하고 자연스럽게 설명해.
        조건이 부족한 상태에서는 함수를 실행하지 말고 부족한 조건 하나만 질문해.
        "추천 동네를 찾아보겠습니다", "잠시만 기다려 주세요"처럼 작업 예정만 말하고 응답을 끝내면 안 돼.
        최종 조건이 모두 확인된 순간에는 반드시 같은 응답 안에서 recommendNeighborhood 함수를 호출해야 해.
        """;

    private final ChatClient chatClient;

    public ChatBotService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultToolNames("recommendNeighborhood")
            .build();
    }

    public String chat(ChatBotRequest request) {
        if (request == null || isBlank(request.message())) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }

        String answer = callChatClient(buildUserPrompt(request));
        if (isWaitingOnlyAnswer(answer)) {
            answer = callChatClient(buildRetryPrompt(request, answer));
        }
        return answer;
    }

    private String callChatClient(String userPrompt) {
        return chatClient.prompt()
            .toolNames("recommendNeighborhood")
            .user(userPrompt)
            .call()
            .content();
    }

    private String buildRetryPrompt(ChatBotRequest request, String previousAnswer) {
        return buildUserPrompt(request) + """

            [중요]
            직전 답변은 아래처럼 대기 안내만 하고 끝났어.
            상담사: %s

            대기 안내로 끝내지 마.
            지금까지의 대화에서 6가지 조건이 모두 있으면 즉시 recommendNeighborhood 함수를 호출해.
            아직 부족한 조건이 있으면 부족한 조건 하나만 질문해.
            """.formatted(previousAnswer);
    }

    private boolean isWaitingOnlyAnswer(String answer) {
        if (isBlank(answer)) {
            return true;
        }
        String normalized = answer.replaceAll("\\s+", "");
        return normalized.contains("찾아보겠습니다")
            || normalized.contains("잠시만기다려")
            || normalized.contains("추천동네를찾")
            || normalized.contains("추천결과를찾");
    }

    private String buildUserPrompt(ChatBotRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            아래는 지금까지의 대화야. 이 내용을 근거로 다음 행동을 결정해.
            - 6가지 조건이 모두 있으면 답변 문장을 먼저 만들지 말고 recommendNeighborhood 함수를 즉시 호출해.
            - 조건이 부족하면 부족한 조건 하나만 질문해.
            - 대기 안내만 하고 끝내지 마.

            """);

        List<ChatMessageDTO> history = request.history();
        if (history != null) {
            for (ChatMessageDTO message : history) {
                if (message == null || isBlank(message.content())) {
                    continue;
                }
                String role = "bot".equalsIgnoreCase(message.role()) ? "상담사" : "사용자";
                prompt.append(role).append(": ").append(message.content().trim()).append('\n');
            }
        }
        prompt.append("사용자: ").append(request.message().trim());
        return prompt.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
