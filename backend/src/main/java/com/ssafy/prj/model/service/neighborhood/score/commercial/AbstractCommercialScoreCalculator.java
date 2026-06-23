package com.ssafy.prj.model.service.neighborhood.score.commercial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

/**
 * 상권 통계 기반 추천 점수 계산기에 공통으로 필요한 구현을 모아 둔 추상 클래스다.
 *
 * <p>카페·외식, 운동·문화, 의료는 DB에서 조회하는 category 값과 화면의 SurveyCategory만 다르고,
 * "동별 업소 수 조회 → 없는 동은 0건 → 상대평가 점수화" 흐름은 완전히 같다. 이 공통 흐름을 한 곳에
 * 두어 새 상권 카테고리를 추가할 때 점수 규칙이 달라지는 일을 방지한다.</p>
 */
abstract class AbstractCommercialScoreCalculator implements CategoryScoreCalculator {

    private final CommercialMetricMapper commercialMetricMapper;
    private final RelativeScorePolicy relativeScorePolicy;
    private final SurveyCategory surveyCategory;
    private final String metricCategory;

    protected AbstractCommercialScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        RelativeScorePolicy relativeScorePolicy,
        SurveyCategory surveyCategory,
        String metricCategory
    ) {
        this.commercialMetricMapper = commercialMetricMapper;
        this.relativeScorePolicy = relativeScorePolicy;
        this.surveyCategory = surveyCategory;
        this.metricCategory = metricCategory;
    }

    /** 이 계산기가 추천 서비스에서 담당하는 설문 우선순위다. */
    @Override
    public SurveyCategory category() {
        return surveyCategory;
    }

    /**
     * 후보 동들의 DB 통계를 조회하고 상대 점수로 변환한다.
     *
     * <p>Mapper SQL은 {@code dongcodes}에서 시작하는 LEFT JOIN이므로 통계 행이 없는 후보 동도
     * 결과에 포함되며, 그 동의 businessCount는 0이다. 따라서 데이터가 아직 없는 동은 자연스럽게
     * 0점을 받으며 추천 계산이 중단되지 않는다.</p>
     */
    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodCommercialCount count : commercialMetricMapper.countByDongCodes(
            dongCodes, metricCategory
        )) {
            countsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }
        return relativeScorePolicy.calculateScores(countsByDongCode);
    }
}
