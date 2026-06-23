package com.ssafy.prj.model.service.neighborhood.score.pet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.commercial.CommercialMetricCategory;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;
import com.ssafy.prj.model.service.neighborhood.score.park.ParkScoreCalculator;

/**
 * 반려인 우선순위 점수를 계산한다.
 *
 * <p>반려인에게 필요한 요소는 서로 단위와 분포가 다르므로, 원시 업소 수를 바로 섞지 않는다.
 * 애완용품점 수, 동물병원 수, 공원 수를 각각 0~100의 상대 점수로 먼저 변환하고 그 점수를 가중합한다.</p>
 *
 * <pre>
 * 반려인 점수 = 애완용품 점수 × 0.25 + 공원 점수 × 0.25 + 동물병원 점수 × 0.50
 * </pre>
 *
 * <p>동물병원은 진료 접근성에 직접 관련된 핵심 지표이므로 가장 큰 비중을 둔다. 가중치 변경이
 * 필요하면 아래 상수만 수정하면 되며, 세 값의 합은 반드시 1.0이 되어야 한다.</p>
 */
@Component
public class PetScoreCalculator implements CategoryScoreCalculator {

    private static final double PET_SUPPLIES_WEIGHT = 0.25;
    private static final double PARK_WEIGHT = 0.25;
    private static final double ANIMAL_HOSPITAL_WEIGHT = 0.50;

    private final CommercialMetricMapper commercialMetricMapper;
    private final ParkScoreCalculator parkScoreCalculator;
    private final RelativeScorePolicy relativeScorePolicy;

    public PetScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        ParkScoreCalculator parkScoreCalculator,
        RelativeScorePolicy relativeScorePolicy
    ) {
        this.commercialMetricMapper = commercialMetricMapper;
        this.parkScoreCalculator = parkScoreCalculator;
        this.relativeScorePolicy = relativeScorePolicy;
    }

    @Override
    public SurveyCategory category() {
        return SurveyCategory.PET;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        // 1. 애완용품점·동물병원의 업소 수를 각각 상대 점수로 만든다.
        Map<String, Double> petSuppliesScores = calculateCommercialRelativeScores(
            dongCodes,
            CommercialMetricCategory.PET_SUPPLIES
        );
        Map<String, Double> animalHospitalScores = calculateCommercialRelativeScores(
            dongCodes,
            CommercialMetricCategory.ANIMAL_HOSPITAL
        );

        // 2. 공원은 이미 독립된 공원 점수 계산기가 있으므로 그 결과를 그대로 재사용한다.
        Map<String, Double> parkScores = parkScoreCalculator.calculateScores(dongCodes);

        // 3. 세 점수를 사용자가 정한 반려인 가중치로 합산한다.
        Map<String, Double> petScores = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            double score = petSuppliesScores.getOrDefault(dongCode, 0.0) * PET_SUPPLIES_WEIGHT
                + parkScores.getOrDefault(dongCode, 0.0) * PARK_WEIGHT
                + animalHospitalScores.getOrDefault(dongCode, 0.0) * ANIMAL_HOSPITAL_WEIGHT;
            petScores.put(dongCode, score);
        }
        return petScores;
    }

    /** 한 상권 원천 지표의 동별 업소 수를 상대 점수로 변환한다. */
    private Map<String, Double> calculateCommercialRelativeScores(
        List<String> dongCodes,
        CommercialMetricCategory metricCategory
    ) {
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            countsByDongCode.put(dongCode, 0);
        }

        for (NeighborhoodCommercialCount count : commercialMetricMapper.countByDongCodes(
            dongCodes,
            metricCategory.getDatabaseCategory()
        )) {
            countsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }
        return relativeScorePolicy.calculateScores(countsByDongCode);
    }
}
