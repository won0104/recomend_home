package com.ssafy.prj.model.service.neighborhood.score.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;
import com.ssafy.prj.model.mapper.neighborhood.SchoolMapper;

@ExtendWith(MockitoExtension.class)
class SchoolScoreCalculatorTest {

    @Mock
    private SchoolMapper schoolMapper;

    private SchoolScoreCalculator schoolScoreCalculator;

    @BeforeEach
    void setUp() {
        schoolScoreCalculator = new SchoolScoreCalculator(schoolMapper);
    }

    @Test
    void 후보_수에_따라_학교_상대평가_점수를_계산한다() {
        List<String> dongCodes = List.of(
            "1111010100", "1135010300", "1121510300", "1111018400", "1114012600",
            "1114016200", "1117010100", "1117012900", "1120010700", "1120011400"
        );
        when(schoolMapper.countByDongCodes(dongCodes)).thenReturn(List.of(
            schoolCount("1111010100", 10),
            schoolCount("1135010300", 8),
            schoolCount("1121510300", 4),
            schoolCount("1111018400", 0),
            schoolCount("1114012600", 0),
            schoolCount("1114016200", 0),
            schoolCount("1117010100", 0),
            schoolCount("1117012900", 0),
            schoolCount("1120010700", 0),
            schoolCount("1120011400", 0)
        ));

        Map<String, Double> result = schoolScoreCalculator.calculateScores(dongCodes);

        assertThat(result).containsEntry("1111010100", 100.0);
        assertThat(result).containsEntry("1135010300", 90.0);
        assertThat(result).containsEntry("1121510300", 80.0);
        assertThat(result).containsEntry("1111018400", 0.0);
    }

    private NeighborhoodSchoolCount schoolCount(String dongCode, int count) {
        NeighborhoodSchoolCount schoolCount = new NeighborhoodSchoolCount();
        schoolCount.setDongCode(dongCode);
        schoolCount.setSchoolCount(count);
        return schoolCount;
    }
}
