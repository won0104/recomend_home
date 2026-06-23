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
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

/**
 * 장보기·생활편의 점수를 계산한다.
 *
 * <p>이 카테고리는 식료품 소매(G205)와 종합 소매(G204)를 같은 생활 편의성으로 본다. 다만 배치
 * 테이블에서는 두 값을 분리 보관한다. 여기서만 두 업소 수를 더한 뒤, 합계 기준으로 후보 동 사이의
 * 상대 점수를 계산한다. 이렇게 하면 원천 데이터를 잃지 않으면서 화면에는 하나의 점수를 제공할 수 있다.</p>
 */
@Component
public class ShoppingScoreCalculator implements CategoryScoreCalculator {

    private final CommercialMetricMapper commercialMetricMapper;
    private final RelativeScorePolicy relativeScorePolicy;

    public ShoppingScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        RelativeScorePolicy relativeScorePolicy
    ) {
        this.commercialMetricMapper = commercialMetricMapper;
        this.relativeScorePolicy = relativeScorePolicy;
    }

    @Override
    public SurveyCategory category() {
        return SurveyCategory.SHOPPING;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        // 각 원천 지표를 별도로 조회한 뒤, 같은 동 코드끼리 업소 수를 합산한다.
        Map<String, Integer> totalCountsByDongCode = initializeCounts(dongCodes);
        addCommercialCounts(totalCountsByDongCode, CommercialMetricCategory.SHOPPING_GROCERY, dongCodes);
        addCommercialCounts(totalCountsByDongCode, CommercialMetricCategory.SHOPPING_GENERAL, dongCodes);

        // 합산한 업소 수를 기준으로 한 번만 상대평가한다.
        return relativeScorePolicy.calculateScores(totalCountsByDongCode);
    }

    /**
     * 후보에 포함됐지만 통계 행이 없는 동도 0건으로 유지하기 위해 모든 후보 코드를 먼저 채운다.
     */
    private Map<String, Integer> initializeCounts(List<String> dongCodes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            counts.put(dongCode, 0);
        }
        return counts;
    }

    /** 한 원천 소매 지표의 조회 결과를 누적 합계 Map에 더한다. */
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
}
