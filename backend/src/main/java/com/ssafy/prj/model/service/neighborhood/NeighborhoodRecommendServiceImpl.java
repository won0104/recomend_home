package com.ssafy.prj.model.service.neighborhood;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.mapper.neighborhood.NeighborhoodRecommendMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NeighborhoodRecommendServiceImpl implements NeighborhoodRecommendService {

    private final NeighborhoodRecommendMapper neighborhoodRecommendMapper;

    @Override
    @Transactional(readOnly = true)
    public NeighborhoodRecommendResponse recommend(NeighborhoodRecommendRequest request) throws Exception {
        validate(request);

        double originLat = request.getLatitude();
        double originLng = request.getLongitude();
        double radiusKm = request.getRadiusKm();
        Integer maxPrice = request.getMaxPrice();

        List<String> legalDongCodes = neighborhoodRecommendMapper.findLegalDongCodes(
                originLat, originLng, radiusKm, maxPrice);

        NeighborhoodRecommendResponse response = new NeighborhoodRecommendResponse();
        response.setLegalDongCodes(legalDongCodes);
        response.setTotalCount(legalDongCodes.size());
        response.setPriorities(normalizePriorities(request.getPriorities()));
        return response;
    }

    private List<String> normalizePriorities(List<String> priorities) {
        if (priorities == null) {
            return List.of();
        }
        return priorities.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(priority -> !priority.isEmpty())
                .map(priority -> priority.toUpperCase(Locale.ROOT))
                .toList();
    }

    private void validate(NeighborhoodRecommendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("추천 조건이 없습니다.");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("기준 위치의 위도/경도가 필요합니다.");
        }
        if (request.getRadiusKm() == null || request.getRadiusKm() <= 0) {
            throw new IllegalArgumentException("반경 범위는 0보다 커야 합니다.");
        }
        if (request.getMaxPrice() != null && request.getMaxPrice() <= 0) {
            throw new IllegalArgumentException("예산 범위는 0보다 커야 합니다.");
        }
    }
}
