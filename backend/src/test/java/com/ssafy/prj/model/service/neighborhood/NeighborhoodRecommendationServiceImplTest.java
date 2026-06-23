package com.ssafy.prj.model.service.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

@ExtendWith(MockitoExtension.class)
class NeighborhoodRecommendationServiceImplTest {

    @Mock
    private CategoryScoreCalculator schoolScoreCalculator;

    @Mock
    private CategoryScoreCalculator cafeScoreCalculator;

    @Mock
    private CategoryScoreCalculator parkScoreCalculator;

    private NeighborhoodRecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new NeighborhoodRecommendationServiceImpl(List.of(
            schoolScoreCalculator,
            cafeScoreCalculator,
            parkScoreCalculator
        ));
    }

    @Test
    void 카테고리별_점수를_가중_합산해_상위_세_동을_추천한다() {
        List<String> dongCodes = List.of("A", "B", "C", "D");
        when(schoolScoreCalculator.category()).thenReturn(SurveyCategory.SCHOOL);
        when(cafeScoreCalculator.category()).thenReturn(SurveyCategory.CAFE);
        when(parkScoreCalculator.category()).thenReturn(SurveyCategory.PARK);
        when(schoolScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 100.0, "B", 80.0, "C", 50.0, "D", 0.0
        ));
        when(cafeScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 40.0, "B", 100.0, "C", 50.0, "D", 0.0
        ));
        when(parkScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 50.0, "B", 0.0, "C", 100.0, "D", 0.0
        ));

        List<RecommendedNeighborhood> result = recommendationService.recommend(
            dongCodes,
            List.of(SurveyCategory.SCHOOL, SurveyCategory.CAFE, SurveyCategory.PARK)
        );

        assertThat(result).extracting(RecommendedNeighborhood::getLawdCd)
            .containsExactly("A", "B", "C");
        assertThat(result).extracting(RecommendedNeighborhood::getScore)
            .containsExactly(72.0, 70.0, 60.0);
        assertThat(result).extracting(RecommendedNeighborhood::getRank)
            .containsExactly(1, 2, 3);
    }

    @Test
    void 후보_동_코드가_없으면_예외를_던진다() {
        assertThatThrownBy(() -> recommendationService.recommend(List.of(), List.of(
            SurveyCategory.SCHOOL,
            SurveyCategory.CAFE,
            SurveyCategory.PARK
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("후보 동 코드 목록은 비어 있을 수 없습니다.");
    }
}
