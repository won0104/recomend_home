package com.ssafy.prj.model.service.neighborhood.score.park;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * parks_for_import.csv를 모두 적재한 로컬 DB 기준 공원 점수를 검증한다.
 * 실행: mvn test -DrunDbIntegrationTests=true -Dtest=ParkScoreCalculatorIntegrationTest
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class ParkScoreCalculatorIntegrationTest {

    @Autowired
    private ParkScoreCalculator parkScoreCalculator;

    @Test
    void 실제_공원_데이터_세_동의_상대평가_점수를_계산한다() {
        // parks_for_import.csv 기준 공원 수: 홍북읍 신경리 68, 정왕동 58, 송도동 56
        List<String> dongCodes = List.of("4480025625", "4139013200", "2818510600");

        Map<String, Double> scores = parkScoreCalculator.calculateScores(dongCodes);

        System.out.println("공원 상대평가 점수: " + scores);

        assertThat(scores).hasSize(3);
        assertThat(scores.get("4480025625")).isCloseTo(100.0, within(0.0001));
        assertThat(scores.get("4139013200")).isCloseTo(100.0, within(0.0001));
        assertThat(scores.get("2818510600")).isCloseTo(100.0, within(0.0001));
    }
}
