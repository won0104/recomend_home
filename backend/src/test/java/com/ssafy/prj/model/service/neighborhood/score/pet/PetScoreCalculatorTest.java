package com.ssafy.prj.model.service.neighborhood.score.pet;

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
import com.ssafy.prj.model.service.neighborhood.score.park.ParkScoreCalculator;

/**
 * 반려인 점수가 애완용품, 공원, 동물병원 점수를 각각 만든 뒤 가중합되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PetScoreCalculatorTest {

    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    @Mock
    private ParkScoreCalculator parkScoreCalculator;

    private PetScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PetScoreCalculator(
            commercialMetricMapper,
            parkScoreCalculator,
            new PercentileScorePolicy()
        );
    }

    @Test
    void 애완용품_공원_동물병원_점수를_025_025_050으로_가중합한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        when(commercialMetricMapper.countByDongCodes(dongCodes, "PET_SUPPLIES"))
            .thenReturn(List.of(
                commercialCount("A", 10),
                commercialCount("B", 0),
                commercialCount("C", 0)
            ));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("PET_SUPPLIES"))
            .thenReturn(List.of(
                commercialCount("A", 10),
                commercialCount("B", 0),
                commercialCount("C", 0),
                commercialCount("D", 5)
            ));

        when(commercialMetricMapper.countByDongCodes(dongCodes, "ANIMAL_HOSPITAL"))
            .thenReturn(List.of(
                commercialCount("A", 5),
                commercialCount("B", 10),
                commercialCount("C", 0)
            ));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("ANIMAL_HOSPITAL"))
            .thenReturn(List.of(
                commercialCount("A", 5),
                commercialCount("B", 10),
                commercialCount("C", 0),
                commercialCount("D", 1)
            ));

        when(parkScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 100.0,
            "B", 75.0,
            "C", 0.0
        ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        assertThat(calculator.category()).isEqualTo(SurveyCategory.PET);
        assertThat(scores.get("A")).isCloseTo(71.48698354997035, Offset.offset(0.000001));
        assertThat(scores.get("B")).isCloseTo(68.75, Offset.offset(0.000001));
        assertThat(scores.get("C")).isEqualTo(0.0);
    }

    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
