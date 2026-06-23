package com.ssafy.prj.model.service.neighborhood.score.commercial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;

/**
 * 음식점 배치가 실제 DB에 저장한 통계로 Mapper와 Calculator를 함께 검증한다.
 *
 * <p>테스트는 DB 데이터를 변경하지 않는다. 음식점 통계가 존재하는 동 세 곳을 읽고,
 * 직접 SQL로 읽은 원본 값과 Mapper·Calculator의 결과를 비교한다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    // 테스트 실행 중 배치가 통계 테이블을 갱신하지 않게 막는다.
    "batch.commercial.enabled=false",
    "batch.commercial.run-on-startup=false",
    "batch.estate.enabled=false"
})
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class CommercialScoreCalculatorIntegrationTest {

    /** 음식점 배치가 I2 데이터를 저장할 때 사용하는 고정 카테고리다. */
    private static final String CATEGORY = "CAFE_RESTAURANT";

    /** Mapper와 별개로 DB 원본 값을 읽어 기대값을 만들기 위한 도구다. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** dong_commercial_metrics를 조회하는 실제 MyBatis Mapper다. */
    @Autowired
    private CommercialMetricMapper commercialMetricMapper;

    /** Mapper 결과를 RelativeScorePolicy로 점수화하는 실제 Calculator다. */
    @Autowired
    private CommercialScoreCalculator commercialScoreCalculator;

    @Test
    void 실제_음식점_통계와_상대_점수를_조회한다() {
        // 1) 실제 음식점 통계가 많은 순으로 세 동을 선택한다.
        List<String> dongCodes = jdbcTemplate.queryForList(
            "SELECT dong_code FROM dong_commercial_metrics "
                + "WHERE category = ? ORDER BY business_count DESC, dong_code LIMIT 3",
            String.class,
            CATEGORY
        );
        assertThat(dongCodes)
            .as("음식점 배치 실행 후 CAFE_RESTAURANT 통계가 세 동 이상 필요합니다")
            .hasSize(3);

        // 2) 원본 테이블을 단순 SQL로 조회해 Mapper가 반환해야 하는 실제 건수를 만든다.
        Map<String, Integer> expectedBusinessCounts = queryRawBusinessCounts(dongCodes);

        // 3) 실제 Mapper가 원본 테이블의 값을 정확히 연결하는지 검증한다.
        List<NeighborhoodCommercialCount> mappedCounts = commercialMetricMapper.countByDongCodes(
            dongCodes,
            CATEGORY
        );
        assertThat(mappedCounts).allSatisfy(item ->
            assertThat(item.getBusinessCount()).isEqualTo(expectedBusinessCounts.get(item.getDongCode()))
        );

        // 4) 실제 Calculator가 같은 건수를 RelativeScorePolicy 규칙으로 점수화하는지 검증한다.
        assertThat(commercialScoreCalculator.calculateScores(dongCodes))
            .containsExactlyInAnyOrderEntriesOf(toExpectedRelativeScores(expectedBusinessCounts));
    }

    /**
     * 음식점 통계 원본을 읽는다. 후보 동에 통계 행이 없을 가능성까지 고려해
     * 먼저 모든 후보를 0으로 채운다.
     */
    private Map<String, Integer> queryRawBusinessCounts(List<String> dongCodes) {
        String placeholders = String.join(",", java.util.Collections.nCopies(dongCodes.size(), "?"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT dong_code, business_count FROM dong_commercial_metrics "
                + "WHERE category = ? AND dong_code IN (" + placeholders + ")",
            combineCategoryAndDongCodes(dongCodes)
        );

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            counts.put(dongCode, 0);
        }
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("dong_code"), ((Number) row.get("business_count")).intValue());
        }
        return counts;
    }

    /** RelativeScorePolicy와 동일한 상대 점수 규칙으로 테스트 기대값을 만든다. */
    private Map<String, Double> toExpectedRelativeScores(Map<String, Integer> counts) {
        Map<String, Double> scores = new LinkedHashMap<>();
        double scoreStep = 100.0 / counts.size();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int currentCount = entry.getValue();
            if (currentCount <= 0) {
                scores.put(entry.getKey(), 0.0);
                continue;
            }
            long betterDongCount = counts.values().stream()
                .filter(count -> count > currentCount)
                .count();
            scores.put(entry.getKey(), 100.0 - betterDongCount * scoreStep);
        }
        return scores;
    }

    /** SQL의 첫 번째 ?(카테고리) 뒤에 동 코드 IN 절 파라미터를 차례로 붙인다. */
    private Object[] combineCategoryAndDongCodes(List<String> dongCodes) {
        Object[] parameters = new Object[dongCodes.size() + 1];
        parameters[0] = CATEGORY;
        for (int index = 0; index < dongCodes.size(); index++) {
            parameters[index + 1] = dongCodes.get(index);
        }
        return parameters;
    }
}
