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

/**
 * 동네 추천 서비스의 "카테고리 가중치 합산"과 "상위 3개 정렬"을 검증하는 단위 테스트다.
 *
 * <p>학교·카페외식·공원의 개별 점수 계산은 각각의 Calculator 책임이다. 따라서 여기서는
 * 각 Calculator가 돌려주는 점수를 가짜로 고정하고, 추천 서비스가 우선순위 가중치대로
 * 합산하여 순위를 만들었는지만 확인한다. 실제 Mapper/DB까지 사용하는 흐름은
 * {@code NeighborhoodRecommendationServiceIntegrationTest}에서 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class NeighborhoodRecommendationServiceImplTest {

    /** 학교 점수 계산기를 가짜로 대체한다. */
    @Mock
    private CategoryScoreCalculator schoolScoreCalculator;

    /** 카페·외식 점수 계산기를 가짜로 대체한다. */
    @Mock
    private CategoryScoreCalculator cafeScoreCalculator;

    /** 공원 점수 계산기를 가짜로 대체한다. */
    @Mock
    private CategoryScoreCalculator parkScoreCalculator;

    private NeighborhoodRecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        // 실제 운영 환경처럼 서비스에 카테고리별 계산기 목록을 등록한다.
        recommendationService = new NeighborhoodRecommendationServiceImpl(List.of(
            schoolScoreCalculator,
            cafeScoreCalculator,
            parkScoreCalculator
        ));
    }

    @Test
    void 우선순위별_점수를_가중치로_합산해_상위_세_동을_추천한다() {
        // 1. 추천 대상으로 비교할 후보 동 4개를 준비한다.
        List<String> dongCodes = List.of("A", "B", "C", "D");

        // 2. 각 Mock Calculator가 담당하는 카테고리를 정한다.
        when(schoolScoreCalculator.category()).thenReturn(SurveyCategory.SCHOOL);
        when(cafeScoreCalculator.category()).thenReturn(SurveyCategory.CAFE);
        when(parkScoreCalculator.category()).thenReturn(SurveyCategory.PARK);

        // 3. 각 카테고리에서 계산되었다고 가정할 점수를 준비한다.
        //    이 값들은 DB 데이터가 아니라, 가중치 합산 로직을 검증하기 위한 테스트 입력값이다.
        when(schoolScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 100.0, "B", 80.0, "C", 50.0, "D", 0.0
        ));
        when(cafeScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 40.0, "B", 100.0, "C", 50.0, "D", 0.0
        ));
        when(parkScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 50.0, "B", 0.0, "C", 100.0, "D", 0.0
        ));

        // 4. 사용자 선택 우선순위를 학교(1순위), 카페외식(2순위), 공원(3순위)으로 전달한다.
        //    서비스 내부 가중치는 각각 0.5, 0.3, 0.2다.
        List<RecommendedNeighborhood> result = recommendationService.recommend(
            dongCodes,
            List.of(SurveyCategory.SCHOOL, SurveyCategory.CAFE, SurveyCategory.PARK)
        );

        // 5. 합산 결과를 직접 읽으면 다음과 같다.
        //    A = 100 * 0.5 + 40 * 0.3 + 50 * 0.2 = 72
        //    B =  80 * 0.5 +100 * 0.3 +  0 * 0.2 = 70
        //    C =  50 * 0.5 + 50 * 0.3 +100 * 0.2 = 60
        //    D = 0점이므로 상위 3개에 포함되지 않는다.
        assertThat(result).extracting(RecommendedNeighborhood::getLawdCd)
            .containsExactly("A", "B", "C");
        assertThat(result).extracting(RecommendedNeighborhood::getScore)
            .containsExactly(72.0, 70.0, 60.0);
        assertThat(result).extracting(RecommendedNeighborhood::getRank)
            .containsExactly(1, 2, 3);
    }

    @Test
    void 후보_동_코드가_비어_있으면_추천을_진행하지_않는다() {
        // 추천 대상이 없으면 점수를 계산할 수 없으므로, 서비스는 명확한 예외를 반환해야 한다.
        assertThatThrownBy(() -> recommendationService.recommend(List.of(), List.of(
            SurveyCategory.SCHOOL,
            SurveyCategory.CAFE,
            SurveyCategory.PARK
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("후보 동 코드 목록은 비어 있을 수 없습니다.");
    }
}
