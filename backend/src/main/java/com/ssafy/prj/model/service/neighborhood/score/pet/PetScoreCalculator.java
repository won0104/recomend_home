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
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;
import com.ssafy.prj.model.service.neighborhood.score.park.ParkScoreCalculator;

/**
 * 반려인 생활환경 점수를 계산한다.
 *
 * <p>반려인 카테고리는 단일 count로 평가하지 않는다. 애완용품점, 공원, 동물병원은 서로 의미가 다른
 * 지표이므로 각각 점수화한 뒤 가중합한다.</p>
 *
 * <pre>
 * 반려인 점수 = 애완용품 점수 × 0.25 + 공원 점수 × 0.25 + 동물병원 점수 × 0.50
 * </pre>
 */
@Component
public class PetScoreCalculator implements CategoryScoreCalculator {

    private static final double PET_SUPPLIES_WEIGHT = 0.25;
    private static final double PARK_WEIGHT = 0.25;
    private static final double ANIMAL_HOSPITAL_WEIGHT = 0.50;

    private final CommercialMetricMapper commercialMetricMapper;
    private final ParkScoreCalculator parkScoreCalculator;
    private final PercentileScorePolicy percentileScorePolicy;

    public PetScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        ParkScoreCalculator parkScoreCalculator,
        PercentileScorePolicy percentileScorePolicy
    ) {
        this.commercialMetricMapper = commercialMetricMapper;
        this.parkScoreCalculator = parkScoreCalculator;
        this.percentileScorePolicy = percentileScorePolicy;
    }

    @Override
    public SurveyCategory category() {
        return SurveyCategory.PET;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        /*
         * 애완용품과 동물병원은 서울 전체 법정동 기준 백분위로 계산한다.
         * 공원은 이미 ParkScoreCalculator에서 개수 구간 점수로 계산하므로 그 결과를 재사용한다.
         */
        Map<String, Double> petSuppliesScores = calculateCommercialPercentileScores(
            dongCodes,
            CommercialMetricCategory.PET_SUPPLIES,
            20.0
        );
        Map<String, Double> animalHospitalScores = calculateCommercialPercentileScores(
            dongCodes,
            CommercialMetricCategory.ANIMAL_HOSPITAL,
            20.0
        );
        Map<String, Double> parkScores = parkScoreCalculator.calculateScores(dongCodes);

        Map<String, Double> petScores = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            double score = petSuppliesScores.getOrDefault(dongCode, 0.0) * PET_SUPPLIES_WEIGHT
                + parkScores.getOrDefault(dongCode, 0.0) * PARK_WEIGHT
                + animalHospitalScores.getOrDefault(dongCode, 0.0) * ANIMAL_HOSPITAL_WEIGHT;
            petScores.put(dongCode, score);
        }
        return petScores;
    }

    /**
     * 애완용품점 또는 동물병원 count를 서울 전체 법정동 기준 백분위 점수로 변환한다.
     */
    private Map<String, Double> calculateCommercialPercentileScores(
        List<String> dongCodes,
        CommercialMetricCategory metricCategory,
        double minimumPositiveScore
    ) {
        Map<String, Integer> targetCountsByDongCode = initializeCounts(dongCodes);
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countByDongCodes(
            dongCodes,
            metricCategory.getDatabaseCategory()
        )) {
            targetCountsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }

        Map<String, Integer> baselineCountsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countAllSeoulDongCodesByCategory(
            metricCategory.getDatabaseCategory()
        )) {
            baselineCountsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }

        return percentileScorePolicy.calculateScores(
            targetCountsByDongCode,
            baselineCountsByDongCode,
            minimumPositiveScore
        );
    }

    /** 조회 결과에 없는 후보 동네도 0점으로 처리하기 위해 먼저 0으로 채운다. */
    private Map<String, Integer> initializeCounts(List<String> dongCodes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            counts.put(dongCode, 0);
        }
        return counts;
    }
}
