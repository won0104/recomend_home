package com.ssafy.prj.model.service.neighborhood;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse.LegalDongCandidate;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse.NeighborhoodSaleCandidate;
import com.ssafy.prj.model.mapper.neighborhood.NeighborhoodRecommendMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NeighborhoodRecommendServiceImpl implements NeighborhoodRecommendService {

    private static final int CANDIDATE_LIMIT = 500;

    private final NeighborhoodRecommendMapper neighborhoodRecommendMapper;

    @Override
    @Transactional(readOnly = true)
    public NeighborhoodRecommendResponse recommend(NeighborhoodRecommendRequest request) throws Exception {
        validate(request);

        double originLat = request.getOrigin().getLatitude();
        double originLng = request.getOrigin().getLongitude();
        double radiusKm = request.getRadiusKm();
        Integer maxPrice = request.getMaxPrice();

        List<NeighborhoodSaleCandidate> candidates = neighborhoodRecommendMapper.findSaleCandidates(
                originLat, originLng, radiusKm, maxPrice, CANDIDATE_LIMIT);
        List<LegalDongCandidate> legalDongs = neighborhoodRecommendMapper.findLegalDongCandidates(
                originLat, originLng, radiusKm, maxPrice);

        NeighborhoodRecommendResponse response = new NeighborhoodRecommendResponse();
        response.setCandidates(candidates);
        response.setLegalDongs(legalDongs);
        response.setTotalCount(candidates.size());
        response.setLifestylePriorities(request.getLifestylePriorities() == null
                ? Collections.emptyList()
                : request.getLifestylePriorities());
        return response;
    }

    private void validate(NeighborhoodRecommendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("추천 조건이 없습니다.");
        }
        if (request.getOrigin() == null
                || request.getOrigin().getLatitude() == null
                || request.getOrigin().getLongitude() == null) {
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
