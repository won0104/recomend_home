package com.ssafy.prj.chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.dto.ChatBotRequest;
import com.ssafy.prj.chat.dto.ChatMessageDTO;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;

@Service
public class ChatBotService {

    private static final Logger log = LoggerFactory.getLogger(ChatBotService.class);
    private static final Pattern RADIUS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*km", Pattern.CASE_INSENSITIVE);
    private static final Pattern EOK_BUDGET_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억");
    private static final Pattern MANWON_BUDGET_PATTERN = Pattern.compile("(\\d+(?:,\\d{3})*|\\d+)\\s*만");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("([가-힣A-Za-z0-9]+(?:역|시청|구청|학교|대학교|병원|공원|터미널))");

    private static final String SYSTEM_PROMPT = """
        너는 상냥하고 꼼꼼한 동네 추천 상담사야.
        말투는 부드럽고 다정하게, 하지만 너무 길지 않게 답해.
        사용자가 읽기 쉽도록 적절한 이모지와 줄바꿈을 사용해. 예: 🏡, 📍, 💰, ✅, 🌿, ☕, 🏫

        [대화 규칙]
        - 사용자가 원하는 주거 조건을 자연스럽게 대화로 수집해.
        - 아래 6가지 조건이 모두 모일 때까지 한 번에 하나씩 질문해.
        - 이미 말한 조건은 다시 묻지 마.
        - 조건을 물을 때는 사용자가 바로 답하기 쉬운 예시를 함께 줘.
        - 조건이 부족한 상태에서는 recommendNeighborhood 함수를 실행하지 마.
        - "추천 동네를 찾아보겠습니다", "잠시만 기다려 주세요"처럼 작업 예정만 말하고 끝내면 안 돼.

        [필수 조건 6가지]
        1. 장소 키워드: 기준 위치 이름. 위도/경도가 아니라 서울역, 삼성역, 서울시청 같은 장소명으로 받아.
        2. 반경: km 단위 숫자.
        3. 예산: 만원 단위 숫자. 3억원은 30000으로 해석해.
        4. 거래유형: 매매, 전세, 월세 등.
        5. 주거형태: 아파트, 원룸, 오피스텔 등.
        6. 우선순위 3개: SCHOOL(학교), CAFE(카페/외식), CULTURE(운동/문화), SHOPPING(장보기/생활편의), PARK(공원/산책), MEDICAL(의료), PET(반려동물) 중 정확히 3개.

        [조건 질문 예시]
        - 📍 기준 위치는 어디로 볼까요? 예: 서울시청, 강남역, 삼성역
        - 🚶 어느 정도 반경까지 괜찮으세요? 예: 3km, 5km, 10km
        - 💰 예산은 어느 정도로 볼까요? 예: 6억 내
        - 🏠 어떤 집을 찾으세요? 예: 매매 아파트, 전세 오피스텔
        - ✨ 생활에서 중요한 것 3가지를 골라주세요: 학교, 카페/외식, 문화, 장보기, 공원, 의료, 반려동물

        [추천 결과 작성 방식]
        조건이 모두 모이면 recommendNeighborhood 함수를 즉시 실행해.
        함수 결과를 받은 뒤에는 아래 형식처럼 보기 좋게 한국어로 정리해.

        🏡 추천 동네 Top 3
        1. 동네명 - 점수
           ✅ 좋은 점: 핵심 강점 1~2개
           💡 이런 분께 좋아요: 사용자 조건과 연결한 짧은 설명

        마지막에는 "더 넓게 볼까요, 아니면 예산/우선순위를 바꿔볼까요?"처럼 자연스러운 후속 질문을 하나만 덧붙여.
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

        int historyCount = request.history() == null ? 0 : request.history().size();
        log.info("[ChatBot] request received. message={}, historyCount={}", request.message(), historyCount);
        ChatConditionSnapshot snapshot = extractConditionSnapshot(request);
        log.info("[ChatBot] condition snapshot from text. locationKeyword={}, radiusKm={}, maxPrice={}, dealType={}, housingType={}, priorities={}, missing={}",
            snapshot.locationKeyword(),
            snapshot.radiusKm(),
            snapshot.maxPrice(),
            snapshot.dealType(),
            snapshot.housingType(),
            snapshot.priorities(),
            snapshot.missingFields());

        String answer = callChatClient(buildUserPrompt(request));
        log.info("[ChatBot] first answer received. waitingOnly={}, answerLength={}",
            isWaitingOnlyAnswer(answer),
            answer == null ? 0 : answer.length());
        if (isWaitingOnlyAnswer(answer)) {
            answer = callChatClient(buildRetryPrompt(request, answer));
            log.info("[ChatBot] retry answer received. answerLength={}", answer == null ? 0 : answer.length());
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
            답변은 상냥한 말투와 적절한 이모지, 짧은 줄바꿈으로 읽기 좋게 작성해.
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
            - 답변은 친근하고 부드러운 말투로 작성해.
            - 긴 문단 하나로 쓰지 말고, 이모지와 줄바꿈을 넣어 읽기 쉽게 정리해.

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

    private ChatConditionSnapshot extractConditionSnapshot(ChatBotRequest request) {
        String conversation = conversationText(request);
        String locationKeyword = extractLocationKeyword(conversation);
        Double radiusKm = extractRadiusKm(conversation);
        Long maxPrice = extractMaxPrice(conversation);
        String dealType = extractFirstMention(conversation, List.of("매매", "전세", "월세"));
        String housingType = extractFirstMention(conversation, List.of("아파트", "오피스텔", "원룸", "빌라", "주택"));
        List<SurveyCategory> priorities = extractPriorities(conversation);

        List<String> missingFields = new ArrayList<>();
        if (isBlank(locationKeyword)) {
            missingFields.add("locationKeyword");
        }
        if (radiusKm == null) {
            missingFields.add("radiusKm");
        }
        if (maxPrice == null) {
            missingFields.add("maxPrice");
        }
        if (isBlank(dealType)) {
            missingFields.add("dealType");
        }
        if (isBlank(housingType)) {
            missingFields.add("housingType");
        }
        if (priorities.size() != 3) {
            missingFields.add("priorities");
        }

        return new ChatConditionSnapshot(
            locationKeyword,
            radiusKm,
            maxPrice,
            dealType,
            housingType,
            priorities,
            missingFields
        );
    }

    private String conversationText(ChatBotRequest request) {
        StringBuilder text = new StringBuilder();
        List<ChatMessageDTO> history = request.history();
        if (history != null) {
            for (ChatMessageDTO message : history) {
                if (message != null && !isBlank(message.content())) {
                    text.append(message.content()).append('\n');
                }
            }
        }
        if (!isBlank(request.message())) {
            text.append(request.message());
        }
        return text.toString();
    }

    private String extractLocationKeyword(String text) {
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        return lastMatch;
    }

    private Double extractRadiusKm(String text) {
        Matcher matcher = RADIUS_PATTERN.matcher(text);
        Double lastMatch = null;
        while (matcher.find()) {
            lastMatch = Double.parseDouble(matcher.group(1));
        }
        return lastMatch;
    }

    private Long extractMaxPrice(String text) {
        Matcher eokMatcher = EOK_BUDGET_PATTERN.matcher(text);
        Long lastMatch = null;
        while (eokMatcher.find()) {
            double eok = Double.parseDouble(eokMatcher.group(1));
            lastMatch = Math.round(eok * 10000);
        }
        if (lastMatch != null) {
            return lastMatch;
        }

        Matcher manwonMatcher = MANWON_BUDGET_PATTERN.matcher(text);
        while (manwonMatcher.find()) {
            lastMatch = Long.parseLong(manwonMatcher.group(1).replace(",", ""));
        }
        return lastMatch;
    }

    private String extractFirstMention(String text, List<String> candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<SurveyCategory> extractPriorities(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        List<SurveyCategory> priorities = new ArrayList<>();
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.SCHOOL, "학교", "교육", "school");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.CAFE, "카페", "외식", "음식", "cafe");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.CULTURE, "문화", "운동", "스포츠", "culture");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.SHOPPING, "장보기", "쇼핑", "마트", "편의", "shopping");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.PARK, "공원", "산책", "park");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.MEDICAL, "의료", "병원", "medical");
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.PET, "반려", "동물", "펫", "pet");
        return priorities;
    }

    private void addPriorityIfMentioned(
        List<SurveyCategory> priorities,
        String text,
        SurveyCategory category,
        String... keywords
    ) {
        if (priorities.contains(category)) {
            return;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                priorities.add(category);
                return;
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ChatConditionSnapshot(
        String locationKeyword,
        Double radiusKm,
        Long maxPrice,
        String dealType,
        String housingType,
        List<SurveyCategory> priorities,
        List<String> missingFields
    ) {
    }
}
