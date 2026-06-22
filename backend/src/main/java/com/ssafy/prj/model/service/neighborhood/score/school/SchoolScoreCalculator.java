package com.ssafy.prj.model.service.neighborhood.score.school;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;
import com.ssafy.prj.model.mapper.neighborhood.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

import lombok.RequiredArgsConstructor;

/**
 * 운영 중인 학교 수를 기준으로 동별 학교 카테고리 점수를 계산한다.
 */
@Component
@RequiredArgsConstructor
public class SchoolScoreCalculator implements CategoryScoreCalculator {

    private final SchoolMapper schoolMapper;

    @Override
    public SurveyCategory category() {
        return SurveyCategory.SCHOOL;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        List<NeighborhoodSchoolCount> schoolCounts = schoolMapper.countByDongCodes(dongCodes);
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodSchoolCount schoolCount : schoolCounts) {
            countsByDongCode.put(schoolCount.getDongCode(), schoolCount.getSchoolCount());
        }

        return calculateRelativeScores(countsByDongCode);
    }

    private Map<String, Double> calculateRelativeScores(Map<String, Integer> countsByDongCode) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (countsByDongCode.isEmpty()) {
            return scores;
        }

        int candidateCount = countsByDongCode.size();
        double scoreStep = 100.0 / candidateCount;
        for (Map.Entry<String, Integer> entry : countsByDongCode.entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count <= 0) {
                scores.put(entry.getKey(), 0.0);
                continue;
            }

            long betterCandidateCount = countsByDongCode.values().stream()
                .filter(candidateCountValue -> candidateCountValue != null && candidateCountValue > count)
                .count();
            scores.put(entry.getKey(), 100.0 - betterCandidateCount * scoreStep);
        }
        return scores;
    }
}
