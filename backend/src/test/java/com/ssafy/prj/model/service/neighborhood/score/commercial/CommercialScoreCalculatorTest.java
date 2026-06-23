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
import com.ssafy.prj.model.service.neighborhood.score.RelativeScorePolicy;

/**
 * {@link CommercialScoreCalculator}가 상권 업소 수를 상대 점수로 바꾸는지만 검증하는 단위 테스트다.
 *
 * <p>이 테스트에서는 DB를 연결하지 않는다. Mapper가 이미 "동별 업소 수"를 조회해 왔다고 가정하고,
 * Mockito로 그 조회 결과를 직접 준비한다. 실제 DB 조회와 Mapper SQL은
 * {@code CommercialScoreCalculatorIntegrationTest}에서 별도로 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class CommercialScoreCalculatorTest {

    /**
     * DB 조회 역할만 가짜로 대체한다.
     * 점수 계산 규칙 자체에만 집중하기 위해서다.
     */
    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    private CommercialScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        // 운영 코드와 동일하게 Mapper와 상대평가 정책을 계산기에 주입한다.
        calculator = new CommercialScoreCalculator(commercialMetricMapper, new RelativeScorePolicy());
    }

    @Test
    void 카페외식_업소_수를_후보_동_사이의_상대_점수로_변환한다() {
        // 1. 추천 후보 동 3개를 준비한다.
        List<String> dongCodes = List.of("1111010100", "1121510300", "1135010300");

        // 2. DB가 조회했다고 가정할 상권 통계값을 준비한다.
        //    - 첫 번째 동: 30개(가장 많음)
        //    - 두 번째 동: 10개
        //    - 세 번째 동: 0개(상권 데이터가 없거나 해당 업종이 없음)
        when(commercialMetricMapper.countByDongCodes(dongCodes, "CAFE_RESTAURANT"))
            .thenReturn(List.of(
                commercialCount("1111010100", 30),
                commercialCount("1121510300", 10),
                commercialCount("1135010300", 0)
            ));

        // 3. 실제 점수 계산기를 실행한다.
        Map<String, Double> scores = calculator.calculateScores(dongCodes);

        // 4. 상대평가 결과를 검증한다.
        //    30개는 후보 중 1위이므로 100점이다.
        assertThat(scores).containsEntry("1111010100", 100.0);

        //    10개는 3개 후보 중 한 곳보다 적으므로 100 - (100 / 3)점이다.
        assertThat(scores).containsEntry("1121510300", 100.0 - (100.0 / 3));

        //    업소 수가 0이면 상대 순위와 관계없이 0점이어야 한다.
        assertThat(scores).containsEntry("1135010300", 0.0);
    }

    /**
     * 테스트에서 Mapper의 반환 형태를 간단히 만들기 위한 보조 메서드다.
     * 실제 SQL 조회 결과의 {@code dong_code}, {@code business_count} 두 컬럼을 표현한다.
     */
    private NeighborhoodCommercialCount commercialCount(String dongCode, int businessCount) {
        NeighborhoodCommercialCount count = new NeighborhoodCommercialCount();
        count.setDongCode(dongCode);
        count.setBusinessCount(businessCount);
        return count;
    }
}
