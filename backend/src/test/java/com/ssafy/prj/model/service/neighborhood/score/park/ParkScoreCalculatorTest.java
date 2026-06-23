package com.ssafy.prj.model.service.neighborhood.score.park;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;
import com.ssafy.prj.model.mapper.neighborhood.ParkMapper;
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

@ExtendWith(MockitoExtension.class)
class ParkScoreCalculatorTest {

    @Mock
    private ParkMapper parkMapper;

    private ParkScoreCalculator parkScoreCalculator;

    @BeforeEach
    void setUp() {
        parkScoreCalculator = new ParkScoreCalculator(parkMapper, new RelativeScorePolicy());
    }

    @Test
    void 후보_수에_따라_공원_상대평가_점수를_계산한다() {
        List<String> dongCodes = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
        );
        when(parkMapper.countByDongCodes(dongCodes)).thenReturn(List.of(
            parkCount("A", 12),
            parkCount("B", 7),
            parkCount("C", 2),
            parkCount("D", 0),
            parkCount("E", 0),
            parkCount("F", 0),
            parkCount("G", 0),
            parkCount("H", 0),
            parkCount("I", 0),
            parkCount("J", 0)
        ));

        Map<String, Double> result = parkScoreCalculator.calculateScores(dongCodes);

        assertThat(result).containsEntry("A", 100.0);
        assertThat(result).containsEntry("B", 90.0);
        assertThat(result).containsEntry("C", 80.0);
        assertThat(result).containsEntry("D", 0.0);
    }

    private NeighborhoodParkCount parkCount(String dongCode, int count) {
        NeighborhoodParkCount parkCount = new NeighborhoodParkCount();
        parkCount.setDongCode(dongCode);
        parkCount.setParkCount(count);
        return parkCount;
    }
}
