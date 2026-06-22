package com.ssafy.prj.model.service.neighborhood.score.park;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;
import com.ssafy.prj.model.mapper.neighborhood.ParkMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

import lombok.RequiredArgsConstructor;

/** 법정동별 공원 수를 기준으로 공원 카테고리 점수를 계산한다. */
@Component
@RequiredArgsConstructor
public class ParkScoreCalculator implements CategoryScoreCalculator {

    private final ParkMapper parkMapper;
    private final RelativeScorePolicy relativeScorePolicy;

    @Override
    public SurveyCategory category() {
        return SurveyCategory.PARK;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        List<NeighborhoodParkCount> parkCounts = parkMapper.countByDongCodes(dongCodes);
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodParkCount parkCount : parkCounts) {
            countsByDongCode.put(parkCount.getDongCode(), parkCount.getParkCount());
        }

        return relativeScorePolicy.calculateScores(countsByDongCode);
    }
}
