package com.ssafy.prj.model.service.neighborhood.score.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/**
 * 장보기 점수가 식료품 소매와 종합 소매를 합산한 뒤 서울 전체 기준 백분위로 계산되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ShoppingScoreCalculatorTest {

    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    private ShoppingScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShoppingScoreCalculator(commercialMetricMapper, new PercentileScorePolicy());
    }

    @Test
    void 식료품소매와_종합소매를_더한_값을_서울_전체_기준_백분위로_점수화한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        when(commercialMetricMapper.countByDongCodes(dongCodes, "SHOPPING_GROCERY"))
            .thenReturn(List.of(
                commercialCount("A", 3),
                commercialCount("B", 3),
                commercialCount("C", 0)
            ));
        when(commercialMetricMapper.countByDongCodes(dongCodes, "SHOPPING_GENERAL"))
            .thenReturn(List.of(
                commercialCount("A", 2),
                commercialCount("B", 4),
                commercialCount("C", 0)
            ));

        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("SHOPPING_GROCERY"))
            .thenReturn(List.of(
                commercialCount("A", 3),
                commercialCount("B", 3),
                commercialCount("C", 0),
                commercialCount("D", 1)
            ));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("SHOPPING_GENERAL"))
            .thenReturn(List.of(
                commercialCount("A", 2),
                commercialCount("B", 4),
                commercialCount("C", 0),
                commercialCount("D", 1)
            ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        assertThat(calculator.category()).isEqualTo(SurveyCategory.SHOPPING);
        assertThat(scores).containsEntry("B", 100.0);
        assertThat(scores.get("A")).isCloseTo(42.973967099940694, Offset.offset(0.000001));
        assertThat(scores).containsEntry("C", 0.0);
    }

    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
