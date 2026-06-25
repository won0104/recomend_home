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
 * 상권 계열 점수 계산기가 후보군 상대평가가 아니라 서울 전체 기준 백분위 점수를 쓰는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CommercialScoreCalculatorTest {

    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    private CommercialScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CommercialScoreCalculator(commercialMetricMapper, new PercentileScorePolicy());
    }

    @Test
    void 카페외식_업소_수를_서울_전체_분포_기준_백분위_점수로_변환한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        when(commercialMetricMapper.countByDongCodes(dongCodes, "CAFE_RESTAURANT"))
            .thenReturn(List.of(
                commercialCount("A", 30),
                commercialCount("B", 10),
                commercialCount("C", 0)
            ));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("CAFE_RESTAURANT"))
            .thenReturn(List.of(
                commercialCount("A", 30),
                commercialCount("B", 10),
                commercialCount("C", 0),
                commercialCount("D", 5)
            ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        assertThat(scores).containsEntry("A", 100.0);
        assertThat(scores.get("B")).isCloseTo(42.973967099940694, Offset.offset(0.000001));
        assertThat(scores).containsEntry("C", 0.0);
    }

    @Test
    void 운동문화_계산기는_CULTURE_SPORTS_통계를_조회한다() {
        CultureSportsScoreCalculator cultureCalculator = new CultureSportsScoreCalculator(
            commercialMetricMapper,
            new PercentileScorePolicy()
        );
        List<String> dongCodes = List.of("A");

        when(commercialMetricMapper.countByDongCodes(dongCodes, "CULTURE_SPORTS"))
            .thenReturn(List.of(commercialCount("A", 12)));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("CULTURE_SPORTS"))
            .thenReturn(List.of(commercialCount("A", 12)));

        assertThat(cultureCalculator.category()).isEqualTo(SurveyCategory.CULTURE);
        assertThat(cultureCalculator.calculateScores(dongCodes)).containsEntry("A", 100.0);
    }

    @Test
    void 의료_계산기는_MEDICAL_통계를_조회하고_최소_양수_점수를_20점으로_둔다() {
        MedicalScoreCalculator medicalCalculator = new MedicalScoreCalculator(
            commercialMetricMapper,
            new PercentileScorePolicy()
        );
        List<String> dongCodes = List.of("A", "B");

        when(commercialMetricMapper.countByDongCodes(dongCodes, "MEDICAL"))
            .thenReturn(List.of(
                commercialCount("A", 8),
                commercialCount("B", 1)
            ));
        when(commercialMetricMapper.countAllSeoulDongCodesByCategory("MEDICAL"))
            .thenReturn(List.of(
                commercialCount("A", 8),
                commercialCount("B", 1),
                commercialCount("C", 0)
            ));

        assertThat(medicalCalculator.category()).isEqualTo(SurveyCategory.MEDICAL);
        assertThat(medicalCalculator.calculateScores(dongCodes)).containsEntry("A", 100.0);
        assertThat(medicalCalculator.calculateScores(dongCodes)).containsEntry("B", 20.0);
    }

    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
