package com.ssafy.prj.chat.service;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.ssafy.prj.chat.dto.ChatBotRecommendResult;
import com.ssafy.prj.chat.dto.ChatBotSlotDTO;
import com.ssafy.prj.chat.dto.KakaoPlace;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.service.neighborhood.NeighborhoodRecommendService;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;

import lombok.RequiredArgsConstructor;

@Component("recommendNeighborhood")
@Description("사용자가 말한 주거 조건 6가지를 기반으로 카카오 장소 검색 후 맞춤 동네 Top3를 추천합니다.")
@RequiredArgsConstructor
public class NeighborhoodRecommendFunction implements Function<ChatBotSlotDTO, ChatBotRecommendResult> {

    private static final Logger log = LoggerFactory.getLogger(NeighborhoodRecommendFunction.class);

    private final KakaoApiService kakaoApiService;
    private final NeighborhoodRecommendService neighborhoodRecommendService;

    @Override
    public ChatBotRecommendResult apply(ChatBotSlotDTO slot) {
        log.info("[ChatBot-Recommend] tool called. slot={}", slot);
        validate(slot);
        log.info("[ChatBot-Recommend] slot validated. locationKeyword={}, radiusKm={}, maxPrice={}, dealType={}, housingType={}, priorities={}",
            slot.locationKeyword(),
            slot.radiusKm(),
            slot.maxPrice(),
            slot.dealType(),
            slot.housingType(),
            slot.priorities());

        KakaoPlace place = kakaoApiService.searchFirstPlace(slot.locationKeyword());
        log.info("[ChatBot-Recommend] kakao place resolved. keyword={}, placeName={}, latitude={}, longitude={}",
            slot.locationKeyword(),
            place.name(),
            place.latitude(),
            place.longitude());

        NeighborhoodRecommendRequest request = new NeighborhoodRecommendRequest();
        request.setLatitude(place.latitude());
        request.setLongitude(place.longitude());
        request.setRadiusKm(slot.radiusKm());
        request.setMaxPrice(Math.toIntExact(slot.maxPrice()));
        request.setDealType(slot.dealType());
        request.setHousingType(slot.housingType());
        request.setPriorities(slot.priorities().stream().map(SurveyCategory::name).toList());
        log.info("[ChatBot-Recommend] converted request. latitude={}, longitude={}, radiusKm={}, maxPrice={}, dealType={}, housingType={}, priorities={}",
            request.getLatitude(),
            request.getLongitude(),
            request.getRadiusKm(),
            request.getMaxPrice(),
            request.getDealType(),
            request.getHousingType(),
            request.getPriorities());

        try {
            NeighborhoodRecommendResponse response = neighborhoodRecommendService.recommend(request);
            log.info("[ChatBot-Recommend] recommendation completed. totalCount={}, recommendationCount={}, priorities={}",
                response.getTotalCount(),
                response.getRecommendations() == null ? 0 : response.getRecommendations().size(),
                response.getPriorities());
            return new ChatBotRecommendResult(
                slot.locationKeyword(),
                place.latitude(),
                place.longitude(),
                slot.radiusKm(),
                slot.maxPrice(),
                slot.dealType(),
                slot.housingType(),
                response
            );
        } catch (Exception exception) {
            log.error("Neighborhood recommendation failed. slot={}, latitude={}, longitude={}, priorities={}",
                slot, place.latitude(), place.longitude(), request.getPriorities(), exception);
            throw new IllegalStateException("동네 추천 실행 중 오류가 발생했습니다: " + rootMessage(exception), exception);
        }
    }

    private void validate(ChatBotSlotDTO slot) {
        if (slot == null) {
            throw new IllegalArgumentException("추천 조건이 없습니다.");
        }
        if (isBlank(slot.locationKeyword())) {
            throw new IllegalArgumentException("장소 키워드가 필요합니다.");
        }
        if (slot.radiusKm() == null || slot.radiusKm() <= 0) {
            throw new IllegalArgumentException("반경은 0보다 커야 합니다.");
        }
        if (slot.maxPrice() == null || slot.maxPrice() <= 0) {
            throw new IllegalArgumentException("예산은 0보다 커야 합니다.");
        }
        if (isBlank(slot.dealType())) {
            throw new IllegalArgumentException("거래 유형이 필요합니다.");
        }
        if (isBlank(slot.housingType())) {
            throw new IllegalArgumentException("주거 형태가 필요합니다.");
        }
        List<SurveyCategory> priorities = slot.priorities();
        if (priorities == null || priorities.size() != 3 || priorities.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("우선순위는 중복 없이 정확히 3개가 필요합니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return isBlank(message) ? current.getClass().getSimpleName() : message;
    }
}
