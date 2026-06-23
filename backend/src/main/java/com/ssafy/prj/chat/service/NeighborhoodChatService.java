package com.ssafy.prj.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.prj.chat.domain.ChatPlaceCandidate;
import com.ssafy.prj.chat.domain.NeighborhoodChatHistoryMessage;
import com.ssafy.prj.chat.domain.NeighborhoodChatSession;
import com.ssafy.prj.chat.domain.NeighborhoodChatStep;
import com.ssafy.prj.chat.dto.LifestyleParseResult;
import com.ssafy.prj.chat.dto.NeighborhoodChatRequest;
import com.ssafy.prj.chat.dto.NeighborhoodChatResponse;
import com.ssafy.prj.chat.repository.NeighborhoodChatHistoryRepository;
import com.ssafy.prj.chat.repository.NeighborhoodChatSessionRepository;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.service.neighborhood.NeighborhoodRecommendService;

@Service
public class NeighborhoodChatService {
    private static final double LARGE_RADIUS_CONFIRM_KM = 30.0;
    private static final int HISTORY_LIMIT = 8;

    private final NeighborhoodChatSessionRepository sessionRepository;
    private final NeighborhoodChatHistoryRepository historyRepository;
    private final NeighborhoodChatParser parser;
    private final NeighborhoodRagService ragService;
    private final KakaoPlaceSearchService kakaoPlaceSearchService;
    private final NeighborhoodRecommendService neighborhoodRecommendService;
    private final NeighborhoodPreferenceMemoryService preferenceMemoryService;
    private final NeighborhoodSlotPlannerService slotPlannerService;

    public NeighborhoodChatService(NeighborhoodChatSessionRepository sessionRepository,
            NeighborhoodChatHistoryRepository historyRepository,
            NeighborhoodChatParser parser,
            NeighborhoodRagService ragService,
            KakaoPlaceSearchService kakaoPlaceSearchService,
            NeighborhoodRecommendService neighborhoodRecommendService,
            NeighborhoodPreferenceMemoryService preferenceMemoryService,
            NeighborhoodSlotPlannerService slotPlannerService,
            NeighborhoodQuestionPromptService ignoredQuestionPromptService) {
        this.sessionRepository = sessionRepository;
        this.historyRepository = historyRepository;
        this.parser = parser;
        this.ragService = ragService;
        this.kakaoPlaceSearchService = kakaoPlaceSearchService;
        this.neighborhoodRecommendService = neighborhoodRecommendService;
        this.preferenceMemoryService = preferenceMemoryService;
        this.slotPlannerService = slotPlannerService;
    }

    public NeighborhoodChatResponse start(String sessionId) {
        String resolvedSessionId = resolveSessionId(sessionId);
        NeighborhoodChatSession session = NeighborhoodChatSession.create(resolvedSessionId);
        sessionRepository.save(session);
        historyRepository.clear(resolvedSessionId);

        String message = slotPlannerService == null
                ? "기준 위치를 알려주세요. 예: 삼성역, 부산역, 서울시청"
                : slotPlannerService.openingQuestion(session, recentHistory(resolvedSessionId));

        historyRepository.appendAssistant(resolvedSessionId, message);
        return NeighborhoodChatResponse.of(session, message);
    }

    public NeighborhoodChatResponse handle(NeighborhoodChatRequest request) throws Exception {
        String sessionId = resolveSessionId(request.getSessionId());
        NeighborhoodChatSession session = sessionRepository.findById(sessionId)
                .orElseGet(() -> NeighborhoodChatSession.create(sessionId));
        String message = request.getMessage() == null ? "" : request.getMessage().trim();

        historyRepository.appendUser(sessionId, message);

        if (message.equalsIgnoreCase("reset") || message.contains("처음부터")) {
            sessionRepository.delete(sessionId);
            historyRepository.clear(sessionId);
            return start(sessionId);
        }

        NeighborhoodChatResponse correction = tryHandleCorrection(session, message);
        if (correction != null) {
            historyRepository.appendAssistant(sessionId, correction.getMessage());
            return correction;
        }

        if (parser.isHelpQuestion(message) && session.getStep() != NeighborhoodChatStep.ASK_PLACE) {
            String answer = ragService.answer(message);
            String reply = answer + "\n\n" + askNextSlot(session, "다음 정보를 이어서 받을게요.", List.of(nextSlotFor(session)));
            historyRepository.appendAssistant(sessionId, reply);
            sessionRepository.save(session);
            return NeighborhoodChatResponse.of(session, reply);
        }

        NeighborhoodChatResponse response = switch (session.getStep()) {
            case ASK_PLACE -> handlePlace(session, message);
            case SELECT_PLACE -> handlePlaceSelection(session, message);
            case ASK_RADIUS -> handleRadius(session, message);
            case ASK_BUDGET -> handleBudget(session, message);
            case ASK_LIFESTYLE -> handleLifestyle(session, message);
            case CONFIRM_LIFESTYLE -> handleLifestyleConfirmation(session, message);
            case DONE -> handleDone(session, message);
        };

        historyRepository.appendAssistant(sessionId, response.getMessage());
        return response;
    }

    private NeighborhoodChatResponse tryHandleCorrection(NeighborhoodChatSession session, String message) {
        if (!parser.isCorrectionRequest(message)) {
            return null;
        }

        String target = parser.correctionTarget(message);
        if (target.isBlank()) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "어떤 항목을 다시 바꿀지 알려주세요.",
                    List.of("PLACE", "RADIUS", "BUDGET", "LIFESTYLE")));
        }

        return switch (target) {
            case "place" -> {
                session.setStep(NeighborhoodChatStep.ASK_PLACE);
                session.setBasePlaceName(null);
                session.setLatitude(null);
                session.setLongitude(null);
                session.setPlaceCandidates(List.of());
                sessionRepository.save(session);
                yield response(session, "기준 위치를 다시 받을게요.\n\n장소명이나 건물명, 역명을 입력해주세요.");
            }
            case "radius" -> {
                session.setStep(NeighborhoodChatStep.ASK_RADIUS);
                session.setRadiusKm(null);
                sessionRepository.save(session);
                yield response(session, askNextSlot(session, "반경을 다시 설정할게요.", List.of("RADIUS")));
            }
            case "budget" -> {
                session.setStep(NeighborhoodChatStep.ASK_BUDGET);
                session.setMaxPrice(null);
                sessionRepository.save(session);
                yield response(session, askNextSlot(session, "예산을 다시 설정할게요.", List.of("BUDGET")));
            }
            case "lifestyle" -> {
                session.setStep(NeighborhoodChatStep.ASK_LIFESTYLE);
                session.setPriorities(List.of());
                sessionRepository.save(session);
                yield response(session, askNextSlot(session, "생활환경 우선순위를 다시 받을게요.", List.of("LIFESTYLE")));
            }
            default -> null;
        };
    }

    private NeighborhoodChatResponse handlePlace(NeighborhoodChatSession session, String message) {
        String keyword = parser.parsePlaceKeyword(message);
        if (keyword.isBlank()) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "기준 위치를 알려주세요.", List.of("PLACE")));
        }

        List<ChatPlaceCandidate> candidates = kakaoPlaceSearchService.search(keyword);
        if (candidates.isEmpty()) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "해당 장소를 찾지 못했어요. 더 구체적으로 말해 주세요.", List.of("PLACE")));
        }

        session.setPlaceCandidates(candidates);
        if (candidates.size() > 1) {
            session.setStep(NeighborhoodChatStep.SELECT_PLACE);
            sessionRepository.save(session);
            return response(session, candidateMessage(keyword, candidates));
        }

        applyPlace(session, candidates.get(0));
        session.setStep(NeighborhoodChatStep.ASK_RADIUS);
        sessionRepository.save(session);
        return response(session, askNextSlot(session, "기준 위치를 " + candidates.get(0).getName() + "으로 설정했어요.", List.of("RADIUS", "BUDGET", "LIFESTYLE")));
    }

    private NeighborhoodChatResponse handlePlaceSelection(NeighborhoodChatSession session, String message) {
        Integer index = parser.parseSelectionIndex(message, session.getPlaceCandidates().size());
        if (index == null) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "장소 후보 번호를 선택해주세요.", List.of("PLACE")));
        }

        ChatPlaceCandidate selected = session.getPlaceCandidates().get(index);
        applyPlace(session, selected);
        session.setStep(NeighborhoodChatStep.ASK_RADIUS);
        sessionRepository.save(session);
        return response(session, askNextSlot(session, "기준 위치를 " + selected.getName() + "으로 설정했어요.", List.of("RADIUS", "BUDGET", "LIFESTYLE")));
    }

    private NeighborhoodChatResponse handleRadius(NeighborhoodChatSession session, String message) {
        if (session.getRadiusKm() != null && parser.isAffirmative(message)) {
            session.setStep(NeighborhoodChatStep.ASK_BUDGET);
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "반경은 " + formatKm(session.getRadiusKm()) + "로 확정할게요.", List.of("BUDGET", "LIFESTYLE")));
        }

        Double radiusKm = parser.parseRadiusKm(message);
        if (radiusKm == null || radiusKm <= 0) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "반경을 이해하지 못했어요.", List.of("RADIUS")));
        }

        session.setRadiusKm(radiusKm);
        if (radiusKm >= LARGE_RADIUS_CONFIRM_KM) {
            sessionRepository.save(session);
            return response(session, """
                    반경을 %s로 이해했어요.

                    범위가 큰 편이라 맞는지 확인해 주세요.
                    - 맞으면: 네, 맞아요
                    - 아니면: 5km 정도로 바꿔줘
                    """.formatted(formatKm(radiusKm)));
        }

        session.setStep(NeighborhoodChatStep.ASK_BUDGET);
        sessionRepository.save(session);
        return response(session, askNextSlot(session, "반경은 " + formatKm(radiusKm) + "로 설정했어요.", List.of("BUDGET", "LIFESTYLE")));
    }

    private NeighborhoodChatResponse handleBudget(NeighborhoodChatSession session, String message) {
        Integer maxPrice = parser.parseBudgetMaxPrice(message);
        session.setMaxPrice(maxPrice);
        session.setStep(NeighborhoodChatStep.ASK_LIFESTYLE);
        sessionRepository.save(session);

        String budgetText = maxPrice == null ? "상관없음" : formatPrice(maxPrice) + " 이하";
        return response(session, askNextSlot(session, "예산은 " + budgetText + "로 설정했어요.", List.of("LIFESTYLE")));
    }

    private NeighborhoodChatResponse handleLifestyle(NeighborhoodChatSession session, String message) {
        LifestyleParseResult result = parser.parseLifestyle(message);
        if (result.getPriorities().isEmpty()) {
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "생활환경 우선순위를 이해하지 못했어요.", List.of("LIFESTYLE")));
        }

        session.setPriorities(result.getPriorities());
        session.setStep(NeighborhoodChatStep.CONFIRM_LIFESTYLE);
        sessionRepository.save(session);
        return response(session, confirmLifestyle(session));
    }

    private NeighborhoodChatResponse handleLifestyleConfirmation(NeighborhoodChatSession session, String message)
            throws Exception {
        if (parser.isAffirmative(message)) {
            return recommend(session);
        }

        Integer rank = parser.parseRankToChange(message);
        LifestyleParseResult parsed = parser.parseLifestyle(message);
        if (!parsed.getPriorities().isEmpty()) {
            List<String> current = new java.util.ArrayList<>(session.getPriorities());
            if (rank != null && rank >= 0 && rank < 3) {
                while (current.size() <= rank) {
                    current.add(null);
                }
                current.set(rank, parsed.getPriorities().get(0));
                current = current.stream().filter(v -> v != null).distinct().limit(3).toList();
            } else {
                current = parsed.getPriorities();
            }
            session.setPriorities(current);
            sessionRepository.save(session);
            return response(session, confirmLifestyle(session));
        }

        sessionRepository.save(session);
        return response(session, "수정 내용을 이해하지 못했어요.\n\n예: 2순위를 공원으로 바꿔줘, 생활환경 다시");
    }

    private NeighborhoodChatResponse handleDone(NeighborhoodChatSession session, String message) {
        if (message.contains("다시") || message.contains("추천")) {
            session.setStep(NeighborhoodChatStep.ASK_PLACE);
            session.setBasePlaceName(null);
            session.setLatitude(null);
            session.setLongitude(null);
            session.setRadiusKm(null);
            session.setMaxPrice(null);
            session.setPriorities(List.of());
            session.setPlaceCandidates(List.of());
            sessionRepository.save(session);
            return response(session, askNextSlot(session, "새 추천을 시작할게요.", List.of("PLACE")));
        }

        String answer = ragService.answer(message);
        sessionRepository.save(session);
        return response(session, answer);
    }

    private NeighborhoodChatResponse recommend(NeighborhoodChatSession session) throws Exception {
        NeighborhoodRecommendRequest request = new NeighborhoodRecommendRequest();
        request.setLatitude(session.getLatitude());
        request.setLongitude(session.getLongitude());
        request.setRadiusKm(session.getRadiusKm());
        request.setMaxPrice(session.getMaxPrice());
        request.setPriorities(session.getPriorities());

        NeighborhoodRecommendResponse recommendation = neighborhoodRecommendService.recommend(request);
        session.setStep(NeighborhoodChatStep.DONE);
        sessionRepository.save(session);
        preferenceMemoryService.rememberCompletedRecommendation(session);

        NeighborhoodChatResponse response = NeighborhoodChatResponse.of(session, recommendationMessage(session, recommendation));
        response.setRecommendation(recommendation);
        return response;
    }

    private void applyPlace(NeighborhoodChatSession session, ChatPlaceCandidate place) {
        session.setBasePlaceName(place.getName());
        session.setLatitude(place.getLatitude());
        session.setLongitude(place.getLongitude());
        session.setPlaceCandidates(List.of(place));
    }

    private String candidateMessage(String keyword, List<ChatPlaceCandidate> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(keyword).append("\"에 대한 후보를 찾았어요.\n\n");
        builder.append("아래 목록에서 기준 위치를 골라주세요.\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            ChatPlaceCandidate candidate = candidates.get(i);
            builder.append(i + 1).append(". ").append(candidate.getName());
            if (candidate.getAddress() != null && !candidate.getAddress().isBlank()) {
                builder.append("\n   ").append(candidate.getAddress());
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String askNextSlot(NeighborhoodChatSession session, String context, List<String> candidateSlots) {
        return slotPlannerService == null
                ? fallbackQuestion(candidateSlots, context)
                : slotPlannerService.nextQuestion(session, recentHistory(session.getSessionId()), candidateSlots, context);
    }

    private String fallbackQuestion(List<String> candidateSlots, String context) {
        String slot = candidateSlots == null || candidateSlots.isEmpty() ? "RADIUS" : candidateSlots.get(0);
        return switch (slot) {
            case "PLACE" -> "기준 위치를 입력해주세요. 예: 삼성역, 부산역, 서울시청";
            case "RADIUS" -> context + "\n\n어느 정도 반경까지 볼까요? 예: 1km, 3km, 5km, 도보권, 차로 20분";
            case "BUDGET" -> context + "\n\n예산은 어느 정도로 볼까요? 예: 8천만원 이하, 1억 이하, 상관없음";
            case "LIFESTYLE" -> context + "\n\n중요하게 보는 생활환경을 최대 3개까지 알려주세요.";
            case "CONFIRM" -> context + "\n\n제가 이해한 내용이 맞는지 확인해 주세요.";
            default -> context;
        };
    }

    private NeighborhoodChatResponse confirmLifestyle(NeighborhoodChatSession session) {
        return response(session, """
                이렇게 이해했어요.

                %s

                맞으면 '네', 틀리면 원하는 항목을 바로 수정해서 말해주세요.
                """.formatted(priorityLines(session.getPriorities())));
    }

    private String recommendationMessage(NeighborhoodChatSession session, NeighborhoodRecommendResponse recommendation) {
        List<String> top = recommendation.getLegalDongCodes().stream().limit(3).toList();
        StringBuilder builder = new StringBuilder();
        builder.append("추천 조건을 바탕으로 후보 동네를 찾았어요.\n\n");
        builder.append("기준 위치: ").append(session.getBasePlaceName()).append("\n");
        builder.append("반경: ").append(formatKm(session.getRadiusKm())).append("\n");
        builder.append("예산: ").append(session.getMaxPrice() == null ? "상관없음" : formatPrice(session.getMaxPrice()) + " 이하").append("\n");
        builder.append("생활환경:\n").append(priorityLines(session.getPriorities()));

        if (top.isEmpty()) {
            builder.append("조건에 맞는 후보가 충분하지 않아요.\n반경이나 예산을 조금 넓혀 다시 시도해 주세요.");
        } else {
            builder.append("추천 후보 Top 3\n");
            for (int i = 0; i < top.size(); i++) {
                builder.append(i + 1).append(". ").append(top.get(i)).append("\n");
            }
        }
        return builder.toString();
    }

    private String priorityLines(List<String> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            return "- 없음\n";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < priorities.size(); i++) {
            builder.append(i + 1).append("순위: ").append(label(priorities.get(i))).append("\n");
        }
        return builder.toString();
    }

    private List<NeighborhoodChatHistoryMessage> recentHistory(String sessionId) {
        return historyRepository.recent(sessionId, HISTORY_LIMIT);
    }

    private NeighborhoodChatResponse response(NeighborhoodChatSession session, String message) {
        sessionRepository.save(session);
        return NeighborhoodChatResponse.of(session, message);
    }

    private String nextSlotFor(NeighborhoodChatSession session) {
        return switch (session.getStep()) {
            case ASK_PLACE, SELECT_PLACE -> "PLACE";
            case ASK_RADIUS -> "RADIUS";
            case ASK_BUDGET -> "BUDGET";
            case ASK_LIFESTYLE -> "LIFESTYLE";
            case CONFIRM_LIFESTYLE, DONE -> "CONFIRM";
        };
    }

    private String label(String category) {
        return switch (category) {
            case "SCHOOL" -> "학교";
            case "CAFE" -> "카페/외식";
            case "CULTURE" -> "운동/문화";
            case "SHOPPING" -> "장보기/생활편의";
            case "PARK" -> "공원/산책";
            case "MEDICAL" -> "의료";
            case "PET" -> "반려동물";
            default -> category;
        };
    }

    private String formatKm(Double radiusKm) {
        return radiusKm == null ? "-" : String.format(java.util.Locale.ROOT, "%.1fkm", radiusKm);
    }

    private String formatPrice(Integer maxPrice) {
        if (maxPrice == null) {
            return "상관없음";
        }
        if (maxPrice >= 10000) {
            double eok = maxPrice / 10000.0;
            return eok == Math.floor(eok) ? ((int) eok) + "억" : String.format(java.util.Locale.ROOT, "%.1f억", eok);
        }
        return maxPrice + "만원";
    }

    private String resolveSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
    }
}
