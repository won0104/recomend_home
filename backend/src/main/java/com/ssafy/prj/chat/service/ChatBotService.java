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
    private static final Pattern DEPOSIT_PATTERN = Pattern.compile("(?:보증금|보증)\\s*(\\d+(?:\\.\\d+)?)(?:\\s*(억|만원|만))?");
    private static final Pattern MONTHLY_RENT_PATTERN = Pattern.compile("(?:월세|월)\\s*(\\d+(?:\\.\\d+)?)(?:\\s*(만원|만))?");
    private static final Pattern SLASH_RENT_PATTERN = Pattern.compile("(\\d+(?:,\\d{3})*)\\s*/\\s*(\\d+(?:,\\d{3})*)");
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
        3. 거래유형: 매매, 전세, 월세 중 하나.
        4. 거래유형별 예산:
           - 매매: dealAmountMax에 최대 매매가를 만원 단위로 넣어. 10억원은 100000.
           - 전세: depositMax에 최대 보증금을 만원 단위로 넣어. 5억원은 50000.
           - 월세: depositMax와 monthlyRentMax를 모두 넣어. 예: 보증금 5000만원, 월세 100만원은 depositMax=5000, monthlyRentMax=100.
        5. 주거형태: 아파트, 원룸, 오피스텔 등.
        6. 우선순위 3개: SCHOOL(학교), CAFE(카페/외식), CULTURE(운동/문화), SHOPPING(장보기/생활편의), PARK(공원/산책), MEDICAL(의료), PET(반려동물) 중 정확히 3개.

        [조건 질문 예시]
        - 📍 기준 위치는 어디로 볼까요? 예: 서울시청, 강남역, 삼성역
        - 🚶 어느 정도 반경까지 괜찮으세요? 예: 3km, 5km, 10km
        - 🏠 거래 유형은 무엇으로 볼까요? 예: 매매, 전세, 월세
        - 💰 매매라면 최대 매매가, 전세라면 최대 보증금, 월세라면 보증금과 월세를 물어봐.
        - 🏡 어떤 주거 형태를 찾으세요? 예: 아파트, 원룸, 오피스텔
        - ✨ 생활에서 중요한 것 3가지를 골라주세요: 학교, 카페/외식, 문화, 장보기, 공원, 의료, 반려동물

        [예산 질문 규칙]
        - 거래유형을 모르면 예산을 먼저 묻지 말고 거래유형부터 물어봐.
        - 매매를 선택하면 "최대 매매가는 얼마까지 괜찮으세요?"라고 물어봐.
        - 전세를 선택하면 "전세 보증금은 최대 얼마까지 괜찮으세요?"라고 물어봐.
        - 월세를 선택하면 "보증금과 월세는 각각 최대 얼마까지 괜찮으세요?"라고 물어봐.
        - 월세에서 보증금이나 월세 중 하나라도 없으면 recommendNeighborhood 함수를 호출하지 마.

        [추천 결과 작성 방식]
        조건이 모두 모이면 recommendNeighborhood 함수를 즉시 실행해.
        함수 결과를 받은 뒤에는 아래 형식처럼 보기 좋게 한국어로 정리해.
        동네명은 반드시 recommendation 안의 sidoName, sigunguName, dongName을 모두 붙인 전체 이름으로 써.
        예: "청담동" 또는 "강남구 청담동"이 아니라 "서울특별시 강남구 청담동"처럼 써.

        🏡 추천 동네 Top 3
        1. 서울특별시 ○○구 ○○동 - 점수
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
        log.info("[ChatBot] condition snapshot from text. locationKeyword={}, radiusKm={}, dealType={}, dealAmountMax={}, depositMax={}, monthlyRentMax={}, housingType={}, priorities={}, missing={}",
            snapshot.locationKeyword(),
            snapshot.radiusKm(),
            snapshot.dealType(),
            snapshot.dealAmountMax(),
            snapshot.depositMax(),
            snapshot.monthlyRentMax(),
            snapshot.housingType(),
            snapshot.priorities(),
            snapshot.missingFields());

        String answer = callChatClient(buildUserPrompt(request, snapshot));
        log.info("[ChatBot] first answer received. waitingOnly={}, answerLength={}",
            isWaitingOnlyAnswer(answer),
            answer == null ? 0 : answer.length());
        if (isWaitingOnlyAnswer(answer)) {
            answer = callChatClient(buildRetryPrompt(request, snapshot, answer));
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

    private String buildRetryPrompt(ChatBotRequest request, ChatConditionSnapshot snapshot, String previousAnswer) {
        return buildUserPrompt(request, snapshot) + """

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

    private String buildUserPrompt(ChatBotRequest request, ChatConditionSnapshot snapshot) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            아래는 지금까지의 대화야. 이 내용을 근거로 다음 행동을 결정해.
            - 6가지 조건이 모두 있으면 답변 문장을 먼저 만들지 말고 recommendNeighborhood 함수를 즉시 호출해.
            - 조건이 부족하면 부족한 조건 하나만 질문해.
            - 대기 안내만 하고 끝내지 마.
            - 답변은 친근하고 부드러운 말투로 작성해.
            - 긴 문단 하나로 쓰지 말고, 이모지와 줄바꿈을 넣어 읽기 쉽게 정리해.

            """);

        appendSnapshotPrompt(prompt, snapshot);

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

    private void appendSnapshotPrompt(StringBuilder prompt, ChatConditionSnapshot snapshot) {
        prompt.append("[서버가 사용자 발화만 보고 추출한 조건]\n");
        prompt.append("- locationKeyword: ").append(snapshot.locationKeyword()).append('\n');
        prompt.append("- radiusKm: ").append(snapshot.radiusKm()).append('\n');
        prompt.append("- dealType: ").append(snapshot.dealType()).append('\n');
        prompt.append("- dealAmountMax: ").append(snapshot.dealAmountMax()).append(" (만원 단위)\n");
        prompt.append("- depositMax: ").append(snapshot.depositMax()).append(" (만원 단위)\n");
        prompt.append("- monthlyRentMax: ").append(snapshot.monthlyRentMax()).append(" (만원 단위)\n");
        prompt.append("- housingType: ").append(snapshot.housingType()).append('\n');
        prompt.append("- priorities: ").append(snapshot.priorities()).append('\n');
        prompt.append("- missing: ").append(snapshot.missingFields()).append("\n\n");
        if (snapshot.missingFields().isEmpty()) {
            prompt.append("""
                위 조건은 서버가 검산한 값이야.
                recommendNeighborhood 함수를 호출할 때 위 값을 그대로 사용해.
                예산 필드는 이미 만원 단위로 변환된 값이므로 다시 계산하지 마.
                매매는 dealAmountMax, 전세는 depositMax, 월세는 depositMax와 monthlyRentMax를 사용해.

                """);
        }
    }

    private ChatConditionSnapshot extractConditionSnapshot(ChatBotRequest request) {
        List<String> userMessages = userMessages(request);
        String conversation = String.join("\n", userMessages);
        String locationKeyword = extractLocationKeyword(userMessages);
        Double radiusKm = extractRadiusKm(conversation);
        String dealType = extractFirstMention(conversation, List.of("매매", "전세", "월세"));
        BudgetSnapshot budget = extractBudgetSnapshot(conversation, dealType);
        String housingType = extractFirstMention(conversation, List.of("아파트", "오피스텔", "원룸", "빌라", "주택"));
        List<SurveyCategory> priorities = extractPriorities(conversation);

        List<String> missingFields = new ArrayList<>();
        if (isBlank(locationKeyword)) {
            missingFields.add("locationKeyword");
        }
        if (radiusKm == null) {
            missingFields.add("radiusKm");
        }
        if (isBlank(dealType)) {
            missingFields.add("dealType");
        } else {
            addMissingBudgetFields(missingFields, dealType, budget);
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
            dealType,
            budget.dealAmountMax(),
            budget.depositMax(),
            budget.monthlyRentMax(),
            housingType,
            priorities,
            missingFields
        );
    }

    private void addMissingBudgetFields(List<String> missingFields, String dealType, BudgetSnapshot budget) {
        if ("매매".equals(dealType)) {
            if (budget.dealAmountMax() == null) {
                missingFields.add("dealAmountMax");
            }
            return;
        }
        if ("전세".equals(dealType)) {
            if (budget.depositMax() == null) {
                missingFields.add("depositMax");
            }
            return;
        }
        if ("월세".equals(dealType)) {
            if (budget.depositMax() == null) {
                missingFields.add("depositMax");
            }
            if (budget.monthlyRentMax() == null) {
                missingFields.add("monthlyRentMax");
            }
        }
    }

    private BudgetSnapshot extractBudgetSnapshot(String text, String dealType) {
        if (isBlank(dealType)) {
            return new BudgetSnapshot(null, null, null);
        }
        if ("매매".equals(dealType)) {
            return new BudgetSnapshot(extractMaxPrice(text), null, null);
        }
        if ("전세".equals(dealType)) {
            return new BudgetSnapshot(null, extractMaxPrice(text), null);
        }
        if ("월세".equals(dealType)) {
            return extractMonthlyRentBudget(text);
        }
        return new BudgetSnapshot(null, null, null);
    }

    private BudgetSnapshot extractMonthlyRentBudget(String text) {
        Long depositMax = extractContextAmount(text, DEPOSIT_PATTERN, true);
        Long monthlyRentMax = extractContextAmount(text, MONTHLY_RENT_PATTERN, false);

        Matcher slashMatcher = SLASH_RENT_PATTERN.matcher(text);
        while (slashMatcher.find()) {
            depositMax = parsePlainManwon(slashMatcher.group(1));
            monthlyRentMax = parsePlainManwon(slashMatcher.group(2));
        }

        if (depositMax == null || monthlyRentMax == null) {
            List<Long> amounts = extractMoneyAmounts(text);
            if (amounts.size() >= 2) {
                if (depositMax == null) {
                    depositMax = amounts.get(amounts.size() - 2);
                }
                if (monthlyRentMax == null) {
                    monthlyRentMax = amounts.get(amounts.size() - 1);
                }
            }
        }

        return new BudgetSnapshot(null, depositMax, monthlyRentMax);
    }

    private Long extractContextAmount(String text, Pattern pattern, boolean allowEokUnit) {
        Matcher matcher = pattern.matcher(text);
        Long lastMatch = null;
        while (matcher.find()) {
            lastMatch = parseAmount(matcher.group(1), matcher.group(2), allowEokUnit);
        }
        return lastMatch;
    }

    private List<Long> extractMoneyAmounts(String text) {
        List<Long> amounts = new ArrayList<>();
        Matcher eokMatcher = EOK_BUDGET_PATTERN.matcher(text);
        while (eokMatcher.find()) {
            amounts.add(Math.round(Double.parseDouble(eokMatcher.group(1)) * 10000));
        }
        Matcher manwonMatcher = MANWON_BUDGET_PATTERN.matcher(text);
        while (manwonMatcher.find()) {
            amounts.add(parsePlainManwon(manwonMatcher.group(1)));
        }
        return amounts;
    }

    private List<String> userMessages(ChatBotRequest request) {
        List<String> messages = new ArrayList<>();
        List<ChatMessageDTO> history = request.history();
        if (history != null) {
            for (ChatMessageDTO message : history) {
                if (message != null && isUserRole(message.role()) && !isBlank(message.content())) {
                    messages.add(message.content().trim());
                }
            }
        }
        if (!isBlank(request.message())) {
            messages.add(request.message().trim());
        }
        return messages;
    }

    private boolean isUserRole(String role) {
        return role == null
            || "user".equalsIgnoreCase(role)
            || "나".equals(role)
            || "사용자".equals(role);
    }

    private String extractLocationKeyword(List<String> userMessages) {
        String joinedText = String.join("\n", userMessages);
        Matcher matcher = LOCATION_PATTERN.matcher(joinedText);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (!isBlank(lastMatch)) {
            return lastMatch;
        }

        for (String message : userMessages) {
            if (isLikelyLocationMessage(message)) {
                lastMatch = message;
            }
        }
        return lastMatch;
    }

    private boolean isLikelyLocationMessage(String message) {
        if (isBlank(message) || message.length() > 30) {
            return false;
        }
        if (!Pattern.compile("[가-힣A-Za-z]").matcher(message).find()) {
            return false;
        }
        return extractRadiusKm(message) == null
            && extractMaxPrice(message) == null
            && extractFirstMention(message, List.of("매매", "전세", "월세")) == null
            && extractFirstMention(message, List.of("아파트", "오피스텔", "원룸", "빌라", "주택")) == null
            && extractPriorities(message).isEmpty();
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
            lastMatch = parseAmount(eokMatcher.group(1), "억", true);
        }
        if (lastMatch != null) {
            return lastMatch;
        }

        Matcher manwonMatcher = MANWON_BUDGET_PATTERN.matcher(text);
        while (manwonMatcher.find()) {
            lastMatch = parsePlainManwon(manwonMatcher.group(1));
        }
        return lastMatch;
    }

    private Long parseAmount(String number, String unit, boolean allowEokUnit) {
        if (isBlank(number)) {
            return null;
        }
        String normalizedUnit = unit == null ? "" : unit.trim();
        double value = Double.parseDouble(number.replace(",", ""));
        if ("억".equals(normalizedUnit)) {
            if (!allowEokUnit) {
                return null;
            }
            return Math.round(value * 10000);
        }
        return Math.round(value);
    }

    private Long parsePlainManwon(String number) {
        if (isBlank(number)) {
            return null;
        }
        return Long.parseLong(number.replace(",", ""));
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
        addPriorityIfMentioned(priorities, normalized, SurveyCategory.TRANSPORT, "교통", "버스", "지하철", "역세권", "transport");
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
        String dealType,
        Long dealAmountMax,
        Long depositMax,
        Long monthlyRentMax,
        String housingType,
        List<SurveyCategory> priorities,
        List<String> missingFields
    ) {
    }

    private record BudgetSnapshot(
        Long dealAmountMax,
        Long depositMax,
        Long monthlyRentMax
    ) {
    }
}
