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
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;
import com.ssafy.prj.model.service.neighborhood.score.park.ParkScoreCalculator;

/**
 * 반려인 점수의 세 구성 요소와 가중치를 검증한다.
 * 공원 계산기와 DB Mapper를 Mock으로 대체해, 반려인 조합 규칙만 명확히 확인한다.
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
            new RelativeScorePolicy()
        );
    }

    @Test
    void 애완용품_공원_동물병원_점수를_025_025_050으로_가중합한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        // 애완용품 업소 수: A가 가장 많으므로 애완용품 점수는 A=100, B=0, C=0이 된다.
        when(commercialMetricMapper.countByDongCodes(dongCodes, "PET_SUPPLIES"))
            .thenReturn(List.of(
                commercialCount("A", 10),
                commercialCount("B", 0),
                commercialCount("C", 0)
            ));

        // 동물병원 업소 수: B=10, A=5, C=0이므로 B=100, A=66.67, C=0이 된다.
        when(commercialMetricMapper.countByDongCodes(dongCodes, "ANIMAL_HOSPITAL"))
            .thenReturn(List.of(
                commercialCount("A", 5),
                commercialCount("B", 10),
                commercialCount("C", 0)
            ));

        // 공원은 ParkScoreCalculator가 계산한 상대 점수를 그대로 가져다 쓴다.
        when(parkScoreCalculator.calculateScores(dongCodes)).thenReturn(Map.of(
            "A", 100.0,
            "B", 20.0,
            "C", 0.0
        ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        // A = 100×0.25 + 100×0.25 + 66.666...×0.50 = 83.333...
        // B =   0×0.25 +  20×0.25 +       100×0.50 = 55
        // C = 모두 0점이다.
        assertThat(calculator.category()).isEqualTo(SurveyCategory.PET);
        assertThat(scores.get("A")).isCloseTo(83.3333333333, Offset.offset(0.000001));
        assertThat(scores.get("B")).isCloseTo(55.0, Offset.offset(0.000001));
        assertThat(scores.get("C")).isEqualTo(0.0);
    }

    /** Mapper가 반환하는 한 행을 간결하게 만들기 위한 테스트 보조 메서드다. */
    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
