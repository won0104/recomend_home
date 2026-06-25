package com.ssafy.prj.model.service.neighborhood.score.commercial;

import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.commercial.CommercialMetricCategory;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/** 카페·외식(I2) 통계를 설문의 {@link SurveyCategory#CAFE} 점수로 변환한다. */
@Component
public class CommercialScoreCalculator extends AbstractCommercialScoreCalculator {

    public CommercialScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        PercentileScorePolicy percentileScorePolicy
    ) {
        super(
            commercialMetricMapper,
            percentileScorePolicy,
            SurveyCategory.CAFE,
            CommercialMetricCategory.CAFE_RESTAURANT.getDatabaseCategory(),
            20.0
        );
    }
}
