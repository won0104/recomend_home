package com.ssafy.prj.model.service.neighborhood.score;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 후보 수를 기준으로 카테고리별 원본 수치를 상대평가 점수로 변환한다.
 */
@Component
public class RelativeScorePolicy {

    public Map<String, Double> calculateScores(Map<String, Integer> countsByDongCode) {
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
