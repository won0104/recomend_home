package com.ssafy.prj.model.service.neighborhood.score.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

/**
 * 장보기 점수가 G205와 G204의 원시 업소 수 합계로 계산되는지 검증한다.
 * DB 조회 자체는 Mock으로 대체하고, 합산 및 상대평가 규칙에만 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class ShoppingScoreCalculatorTest {

    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    private ShoppingScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShoppingScoreCalculator(commercialMetricMapper, new RelativeScorePolicy());
    }

    @Test
    void 식료품소매와_종합소매_업소_수를_더한_값으로_점수를_계산한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        // G205 식료품 소매: A=3, B=3, C=0
        when(commercialMetricMapper.countByDongCodes(dongCodes, "SHOPPING_GROCERY"))
            .thenReturn(List.of(
                commercialCount("A", 3),
                commercialCount("B", 3),
                commercialCount("C", 0)
            ));

        // G204 종합 소매: A=2, B=4, C=0
        when(commercialMetricMapper.countByDongCodes(dongCodes, "SHOPPING_GENERAL"))
            .thenReturn(List.of(
                commercialCount("A", 2),
                commercialCount("B", 4),
                commercialCount("C", 0)
            ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        // 합계는 A=5, B=7, C=0이다. B가 1위(100점), A가 2위(66.67점), C는 0점이다.
        assertThat(calculator.category()).isEqualTo(SurveyCategory.SHOPPING);
        assertThat(scores).containsEntry("B", 100.0);
        assertThat(scores).containsEntry("A", 100.0 - (100.0 / 3));
        assertThat(scores).containsEntry("C", 0.0);
    }

    /** Mapper가 반환하는 한 행을 간결하게 만들기 위한 테스트 보조 메서드다. */
    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
