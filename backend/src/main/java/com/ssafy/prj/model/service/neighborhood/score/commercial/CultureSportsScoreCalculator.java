package com.ssafy.prj.model.service.neighborhood.score.commercial;

import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.commercial.CommercialMetricCategory;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/** 운동·문화(R1) 통계를 설문의 {@link SurveyCategory#CULTURE} 점수로 변환한다. */
@Component
public class CultureSportsScoreCalculator extends AbstractCommercialScoreCalculator {

    public CultureSportsScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        PercentileScorePolicy percentileScorePolicy
    ) {
        super(
            commercialMetricMapper,
            percentileScorePolicy,
            SurveyCategory.CULTURE,
            CommercialMetricCategory.CULTURE_SPORTS.getDatabaseCategory(),
            20.0
        );
    }
}
