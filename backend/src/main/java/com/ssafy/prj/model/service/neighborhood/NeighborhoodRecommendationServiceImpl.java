package com.ssafy.prj.model.service.neighborhood;

import java.util.List;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리별 점수를 수집하고, 가중치를 적용해 상위 3개 동네를 추천한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NeighborhoodRecommendationServiceImpl implements NeighborhoodRecommendationService {

    private static final List<Double> PRIORITY_WEIGHTS = List.of(0.5, 0.3, 0.2);
    private static final int RECOMMENDATION_LIMIT = 3;

    private final List<CategoryScoreCalculator> scoreCalculators;

    @Override
    public List<RecommendedNeighborhood> recommend(
        List<String> dongCodes,
        List<SurveyCategory> priorities
    ) {
        validateRequest(dongCodes, priorities);

        Map<SurveyCategory, Map<String, Double>> scoresByCategory =
            new EnumMap<>(SurveyCategory.class);
        for (SurveyCategory category : priorities) {
            scoresByCategory.put(category, getCategoryScores(category, dongCodes));
        }

        Map<String, Double> totalScores = calculateWeightedScores(
            dongCodes,
            priorities,
            scoresByCategory
        );
        return selectTopRecommendations(totalScores);
    }

    private Map<String, Double> getCategoryScores(
        SurveyCategory category,
        List<String> dongCodes
    ) {
        return scoreCalculators.stream()
            .filter(calculator -> calculator.category() == category)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 카테고리입니다: " + category))
            .calculateScores(dongCodes);
    }

    private Map<String, Double> calculateWeightedScores(
        List<String> dongCodes,
        List<SurveyCategory> priorities,
        Map<SurveyCategory, Map<String, Double>> scoresByCategory
    ) {
        Map<String, Double> totalScores = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            double totalScore = 0.0;
            for (int index = 0; index < priorities.size(); index++) {
                SurveyCategory category = priorities.get(index);
                double categoryScore = scoresByCategory.get(category)
                    .getOrDefault(dongCode, 0.0);
                totalScore += categoryScore * PRIORITY_WEIGHTS.get(index);
            }
            totalScores.put(dongCode, totalScore);
        }
        return totalScores;
    }

    private List<RecommendedNeighborhood> selectTopRecommendations(
        Map<String, Double> totalScores
    ) {
        List<Map.Entry<String, Double>> sortedScores = totalScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(java.util.Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(RECOMMENDATION_LIMIT)
            .toList();

        return java.util.stream.IntStream.range(0, sortedScores.size())
            .mapToObj(index -> {
                Map.Entry<String, Double> score = sortedScores.get(index);
                RecommendedNeighborhood recommendation = new RecommendedNeighborhood();
                recommendation.setLawdCd(score.getKey());
                recommendation.setRank(index + 1);
                recommendation.setScore(score.getValue());
                return recommendation;
            })
            .toList();
    }

    private void validateRequest(List<String> dongCodes, List<SurveyCategory> priorities) {
        if (dongCodes == null || dongCodes.isEmpty()) {
            throw new IllegalArgumentException("후보 동 코드 목록은 비어 있을 수 없습니다.");
        }
        if (priorities == null || priorities.size() != PRIORITY_WEIGHTS.size()) {
            throw new IllegalArgumentException("우선순위 카테고리는 정확히 3개여야 합니다.");
        }

        Set<SurveyCategory> uniquePriorities = new HashSet<>(priorities);
        if (uniquePriorities.size() != priorities.size()) {
            throw new IllegalArgumentException("우선순위 카테고리는 중복될 수 없습니다.");
        }
    }
}
