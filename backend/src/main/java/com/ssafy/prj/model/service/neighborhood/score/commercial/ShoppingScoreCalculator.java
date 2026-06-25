package com.ssafy.prj.model.service.neighborhood.score.commercial;

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

/**
 * 장보기·생활편의 점수를 계산한다.
 *
 * <p>이 카테고리는 식료품 소매(G205)와 종합 소매(G204)를 하나의 생활 편의 지표로 합산한다.
 * 합산한 count는 후보군 내부 상대평가가 아니라 서울 전체 법정동의 합산 분포를 기준으로 백분위 점수화한다.</p>
 */
@Component
public class ShoppingScoreCalculator implements CategoryScoreCalculator {

    private static final double MINIMUM_POSITIVE_SCORE = 20.0;

    private final CommercialMetricMapper commercialMetricMapper;
    private final PercentileScorePolicy percentileScorePolicy;

    public ShoppingScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        PercentileScorePolicy percentileScorePolicy
    ) {
        this.commercialMetricMapper = commercialMetricMapper;
        this.percentileScorePolicy = percentileScorePolicy;
    }

    @Override
    public SurveyCategory category() {
        return SurveyCategory.SHOPPING;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        /*
         * 1. 추천 후보 동네의 식료품 소매 + 종합 소매 count를 합산한다.
         * 2. 서울 전체 법정동의 식료품 소매 + 종합 소매 count를 같은 방식으로 합산한다.
         * 3. 후보 동네 count를 서울 전체 합산 분포 기준 백분위 점수로 바꾼다.
         */
        Map<String, Integer> targetCountsByDongCode = initializeCounts(dongCodes);
        addCommercialCounts(targetCountsByDongCode, CommercialMetricCategory.SHOPPING_GROCERY, dongCodes);
        addCommercialCounts(targetCountsByDongCode, CommercialMetricCategory.SHOPPING_GENERAL, dongCodes);

        Map<String, Integer> baselineCountsByDongCode = initializeAllSeoulCounts();
        addAllSeoulCommercialCounts(baselineCountsByDongCode, CommercialMetricCategory.SHOPPING_GROCERY);
        addAllSeoulCommercialCounts(baselineCountsByDongCode, CommercialMetricCategory.SHOPPING_GENERAL);

        return percentileScorePolicy.calculateScores(
            targetCountsByDongCode,
            baselineCountsByDongCode,
            MINIMUM_POSITIVE_SCORE
        );
    }

    /** 추천 후보 동네 중 상권 집계가 없는 동네도 0점으로 처리하기 위해 먼저 0으로 채운다. */
    private Map<String, Integer> initializeCounts(List<String> dongCodes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            counts.put(dongCode, 0);
        }
        return counts;
    }

    /** 후보 동네 count Map에 특정 상권 category의 count를 누적한다. */
    private void addCommercialCounts(
        Map<String, Integer> totalCountsByDongCode,
        CommercialMetricCategory metricCategory,
        List<String> dongCodes
    ) {
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countByDongCodes(
            dongCodes,
            metricCategory.getDatabaseCategory()
        )) {
            totalCountsByDongCode.merge(count.getDongCode(), count.getBusinessCount(), Integer::sum);
        }
    }

    /**
     * 서울 전체 법정동을 기준 모집단으로 쓰기 위해 전체 동네 코드를 먼저 0으로 채운다.
     *
     * <p>식료품 소매와 종합 소매는 같은 dongcodes 기준으로 조회되므로,
     * 식료품 소매 조회 결과를 전체 법정동 목록처럼 사용해도 된다.</p>
     */
    private Map<String, Integer> initializeAllSeoulCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countAllSeoulDongCodesByCategory(
            CommercialMetricCategory.SHOPPING_GROCERY.getDatabaseCategory()
        )) {
            counts.put(count.getDongCode(), 0);
        }
        return counts;
    }

    /** 서울 전체 법정동 count Map에 특정 상권 category의 count를 누적한다. */
    private void addAllSeoulCommercialCounts(
        Map<String, Integer> totalCountsByDongCode,
        CommercialMetricCategory metricCategory
    ) {
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countAllSeoulDongCodesByCategory(
            metricCategory.getDatabaseCategory()
        )) {
            totalCountsByDongCode.merge(count.getDongCode(), count.getBusinessCount(), Integer::sum);
        }
    }
}
