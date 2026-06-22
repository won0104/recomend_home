package com.ssafy.prj.model.service.neighborhood.score;

import java.util.List;
import java.util.Map;

import com.ssafy.prj.model.service.neighborhood.SurveyCategory;

/**
 * 하나의 생활 카테고리에 대한 동별 점수를 계산한다.
 */
public interface CategoryScoreCalculator {

    SurveyCategory category();

    Map<String, Double> calculateScores(List<String> dongCodes);
}
