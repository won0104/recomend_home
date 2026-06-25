package com.ssafy.prj.model.service.neighborhood.score.transport;

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

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodBusStopCount;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSubwayCount;
import com.ssafy.prj.model.mapper.neighborhood.TransportMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/** 교통 점수가 버스 정류장 점수와 지하철 점수를 가중합하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class TransportScoreCalculatorTest {

    @Mock
    private TransportMetricMapper transportMetricMapper;

    private TransportScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TransportScoreCalculator(transportMetricMapper, new PercentileScorePolicy());
    }

    @Test
    void 버스정류장_점수와_지하철_점수를_035_065로_가중합한다() {
        List<String> dongCodes = List.of("A", "B", "C");

        when(transportMetricMapper.countBusStopsByDongCodes(dongCodes)).thenReturn(List.of(
            busStopCount("A", 10),
            busStopCount("B", 5),
            busStopCount("C", 0)
        ));
        when(transportMetricMapper.countAllSeoulBusStops()).thenReturn(List.of(
            busStopCount("A", 10),
            busStopCount("B", 5),
            busStopCount("C", 0),
            busStopCount("D", 2)
        ));

        when(transportMetricMapper.countSubwaysByDongCodes(dongCodes)).thenReturn(List.of(
            subwayCount("A", 2, 2, 4),
            subwayCount("B", 1, 1, 2),
            subwayCount("C", 0, 0, 0)
        ));
        when(transportMetricMapper.countAllSeoulSubways()).thenReturn(List.of(
            subwayCount("A", 2, 2, 4),
            subwayCount("B", 1, 1, 2),
            subwayCount("C", 0, 0, 0),
            subwayCount("D", 1, 1, 1)
        ));

        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        assertThat(calculator.category()).isEqualTo(SurveyCategory.TRANSPORT);
        assertThat(scores.get("A")).isEqualTo(100.0);
        assertThat(scores.get("B")).isCloseTo(42.973967099940694, Offset.offset(0.000001));
        assertThat(scores.get("C")).isEqualTo(0.0);
    }

    private NeighborhoodBusStopCount busStopCount(String dongCode, int count) {
        NeighborhoodBusStopCount result = new NeighborhoodBusStopCount();
        result.setDongCode(dongCode);
        result.setBusStopCount(count);
        return result;
    }

    private NeighborhoodSubwayCount subwayCount(
        String dongCode,
        int stationCount,
        int lineCount,
        int subwayIndex
    ) {
        NeighborhoodSubwayCount result = new NeighborhoodSubwayCount();
        result.setDongCode(dongCode);
        result.setStationCount(stationCount);
        result.setLineCount(lineCount);
        result.setSubwayIndex(subwayIndex);
        return result;
    }
}
