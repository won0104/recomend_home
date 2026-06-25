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
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/**
 * 실제 DB에 적재된 카페·외식 통계를 기준으로 Mapper와 Calculator를 함께 검증한다.
 *
 * <p>이 테스트는 로컬 DB 데이터가 필요하므로 기본 테스트에서는 실행하지 않고,
 * {@code -DrunDbIntegrationTests=true}를 붙였을 때만 실행된다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "batch.commercial.enabled=false",
    "batch.commercial.run-on-startup=false",
    "batch.estate.enabled=false"
})
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class CommercialScoreCalculatorIntegrationTest {

    private static final String CATEGORY = "CAFE_RESTAURANT";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CommercialMetricMapper commercialMetricMapper;

    @Autowired
    private CommercialScoreCalculator commercialScoreCalculator;

    @Autowired
    private PercentileScorePolicy percentileScorePolicy;

    @Test
    void 실제_카페외식_통계를_서울_전체_분포_기준_백분위_점수로_조회한다() {
        List<String> dongCodes = jdbcTemplate.queryForList(
            "SELECT dong_code FROM dong_commercial_metrics "
                + "WHERE category = ? ORDER BY business_count DESC, dong_code LIMIT 3",
            String.class,
            CATEGORY
        );
        assertThat(dongCodes)
            .as("CAFE_RESTAURANT 배치 데이터가 최소 3개 이상 필요합니다.")
            .hasSize(3);

        Map<String, Integer> targetCounts = toCountMap(commercialMetricMapper.countByDongCodes(
            dongCodes,
            CATEGORY
        ));
        Map<String, Integer> baselineCounts = toCountMap(
            commercialMetricMapper.countAllSeoulDongCodesByCategory(CATEGORY)
        );

        assertThat(commercialScoreCalculator.calculateScores(dongCodes))
            .containsExactlyInAnyOrderEntriesOf(percentileScorePolicy.calculateScores(
                targetCounts,
                baselineCounts,
                20.0
            ));
    }

    /** Mapper DTO 목록을 점수 정책이 사용하기 쉬운 dongCode → count Map으로 바꾼다. */
    private Map<String, Integer> toCountMap(List<NeighborhoodCommercialCount> counts) {
        Map<String, Integer> countsByDongCode = new LinkedHashMap<>();
        for (NeighborhoodCommercialCount count : counts) {
            countsByDongCode.put(count.getDongCode(), count.getBusinessCount());
        }
        return countsByDongCode;
    }
}
