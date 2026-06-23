package com.ssafy.prj.model.service.neighborhood.score.commercial;

import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.commercial.CommercialMetricCategory;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

/** 보건·의료(Q1) 통계를 설문의 {@link SurveyCategory#MEDICAL} 점수로 변환한다. */
@Component
public class MedicalScoreCalculator extends AbstractCommercialScoreCalculator {

    public MedicalScoreCalculator(
        CommercialMetricMapper commercialMetricMapper,
        RelativeScorePolicy relativeScorePolicy
    ) {
        super(
            commercialMetricMapper,
            relativeScorePolicy,
            SurveyCategory.MEDICAL,
            CommercialMetricCategory.MEDICAL.getDatabaseCategory()
        );
    }
}
