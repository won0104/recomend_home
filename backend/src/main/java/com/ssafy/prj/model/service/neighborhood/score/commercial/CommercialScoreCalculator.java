package com.ssafy.prj.model.service.neighborhood.score.commercial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommercialScoreCalculator implements CategoryScoreCalculator {

    private static final String CAFE_RESTAURANT = "CAFE_RESTAURANT";

    private final CommercialMetricMapper commercialMetricMapper;
    private final RelativeScorePolicy relativeScorePolicy;

    @Override
    public SurveyCategory category() {
        return SurveyCategory.CAFE;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countByDongCodes(
            dongCodes, CAFE_RESTAURANT
        )) {
            countsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }
        return relativeScorePolicy.calculateScores(countsByDongCode);
    }
}
