package com.ssafy.prj.model.service.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

/**
 * 실제 DB에 적재된 데이터를 이용해 최종 동네 추천 흐름을 검증한다.
 *
 * <p>이 테스트는 데이터를 INSERT, UPDATE, DELETE하지 않는다. 따라서 현재 DB의
 * schools, parks, dong_commercial_metrics 테이블에 있는 값 그대로 점수를 계산한다.</p>
 *
 * <p>실행 조건: 음식점 배치가 성공해 CAFE_RESTAURANT 통계가 하나 이상 있어야 한다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    // 통합 테스트 중 외부 API 배치가 실행되어 DB 값이 바뀌지 않도록 모두 비활성화한다.
    "batch.commercial.enabled=false",
    "batch.commercial.run-on-startup=false",
    "batch.estate.enabled=false"
})
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class NeighborhoodRecommendationServiceIntegrationTest {

    /** dong_commercial_metrics에 저장되는 카페·외식 통계 카테고리 값이다. */
    private static final String COMMERCIAL_CATEGORY = "CAFE_RESTAURANT";

    /**
     * Mapper가 아닌 단순 SQL로 DB의 원본 개수를 읽는다.
     * Mapper 결과를 다시 같은 Mapper로 검증하는 순환 검증을 피하기 위해 사용한다.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 학교·음식점·공원 Calculator를 실제로 주입받는 최종 추천 서비스다. */
    @Autowired
    private NeighborhoodRecommendationService recommendationService;

    @Test
    void 실제_DB_데이터로_학교_음식점_공원_추천_점수를_합산한다() {
        // 1) 실제 음식점 통계가 있는 동 1개와, 음식점 통계가 없는 학교 동 2개를 고른다.
        //    음식점 데이터가 없을 때 0점이 되는 경우까지 함께 검증하기 위한 구성이다.
        List<String> candidateDongCodes = findRealCandidateDongCodes();
        assertThat(candidateDongCodes)
            .as("음식점 통계가 있는 동 1개와 없는 학교 동 2개가 필요합니다")
            .hasSize(3);

        // 2) 각 테이블에서 실제 개수를 직접 읽는다. 없는 카테고리는 아래 helper가 0으로 채운다.
        Map<String, Integer> schoolCounts = queryMetricCounts(
            "SELECT dong_code, COUNT(*) AS metric_count FROM schools "
                + "WHERE operation_status = '운영' AND dong_code IN (%s) GROUP BY dong_code",
            candidateDongCodes
        );
        Map<String, Integer> parkCounts = queryMetricCounts(
            "SELECT dong_code, COUNT(*) AS metric_count FROM parks "
                + "WHERE dong_code IN (%s) GROUP BY dong_code",
            candidateDongCodes
        );
        Map<String, Integer> commercialCounts = queryMetricCounts(
            "SELECT dong_code, business_count AS metric_count FROM dong_commercial_metrics "
                + "WHERE category = ? AND dong_code IN (%s)",
            candidateDongCodes,
            COMMERCIAL_CATEGORY
        );

        // 3) 서비스와 같은 상대 점수 규칙과 1·2·3순위 가중치를 테스트 안에서 독립 계산한다.
        Map<String, Double> expectedScores = calculateExpectedTotalScores(
            toRelativeScores(schoolCounts),
            toRelativeScores(commercialCounts),
            toRelativeScores(parkCounts)
        );
        List<String> expectedDongOrder = sortTopThree(expectedScores);

        // 4) 실제 SchoolMapper, CommercialMetricMapper, ParkMapper와 세 Calculator를 거쳐
        //    NeighborhoodRecommendationServiceImpl.recommend(...)를 실행한다.
        List<RecommendedNeighborhood> recommendations = recommendationService.recommend(
            candidateDongCodes,
            List.of(SurveyCategory.SCHOOL, SurveyCategory.CAFE, SurveyCategory.PARK)
        );

        // 5) DB 원본 데이터로 계산한 기대 순서·점수와 서비스 반환값을 비교한다.
        assertThat(recommendations).extracting(RecommendedNeighborhood::getLawdCd)
            .containsExactlyElementsOf(expectedDongOrder);
        for (int index = 0; index < recommendations.size(); index++) {
            RecommendedNeighborhood recommendation = recommendations.get(index);
            assertThat(recommendation.getRank()).isEqualTo(index + 1);
            assertThat(recommendation.getScore())
                .isCloseTo(expectedScores.get(recommendation.getLawdCd()), within(0.0001));
        }
    }

    /**
     * 실제 DB에서 테스트 후보 세 동을 선택한다.
     *
     * <ul>
     *   <li>첫 번째 동: 음식점 통계가 존재해 카페·외식 점수가 계산되는 경우</li>
     *   <li>나머지 두 동: 운영 학교는 있지만 음식점 통계가 없어 카페·외식 0점이 되는 경우</li>
     * </ul>
     */
    private List<String> findRealCandidateDongCodes() {
        String sql = "SELECT candidates.dong_code FROM ("
            + "(SELECT dong_code, 0 AS selection_order FROM dong_commercial_metrics "
            + "WHERE category = ? AND business_count > 0 ORDER BY business_count DESC, dong_code LIMIT 1) "
            + "UNION ALL "
            + "(SELECT s.dong_code, 1 AS selection_order FROM schools s "
            + "WHERE s.operation_status = '운영' "
            + "AND NOT EXISTS (SELECT 1 FROM dong_commercial_metrics m "
            + "WHERE m.dong_code = s.dong_code AND m.category = ?) "
            + "GROUP BY s.dong_code ORDER BY s.dong_code LIMIT 2)"
            + ") candidates ORDER BY candidates.selection_order, candidates.dong_code";

        return jdbcTemplate.queryForList(
            sql,
            String.class,
            COMMERCIAL_CATEGORY,
            COMMERCIAL_CATEGORY
        );
    }

    /**
     * SQL 결과를 {@code 동 코드 -> 개수} 형태로 바꾼다.
     * SQL 결과에 행이 없는 후보도 Map에 0으로 넣어 '시설이 없음 = 0점' 규칙을 보장한다.
     */
    private Map<String, Integer> queryMetricCounts(
        String sqlTemplate,
        List<String> dongCodes,
        Object... prefixParameters
    ) {
        String placeholders = String.join(",", java.util.Collections.nCopies(dongCodes.size(), "?"));
        Object[] parameters = combineParameters(prefixParameters, dongCodes);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            sqlTemplate.formatted(placeholders),
            parameters
        );

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            counts.put(dongCode, 0);
        }
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("dong_code"), ((Number) row.get("metric_count")).intValue());
        }
        return counts;
    }

    /**
     * RelativeScorePolicy와 같은 규칙으로 기대 상대 점수를 계산한다.
     * 양수 개수끼리만 순위를 비교하며, 개수가 0이면 무조건 0점이다.
     */
    private Map<String, Double> toRelativeScores(Map<String, Integer> counts) {
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

    /** 학교 50%, 음식점 30%, 공원 20%를 적용해 최종 기대 점수를 계산한다. */
    private Map<String, Double> calculateExpectedTotalScores(
        Map<String, Double> schoolScores,
        Map<String, Double> commercialScores,
        Map<String, Double> parkScores
    ) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (String dongCode : schoolScores.keySet()) {
            double total = schoolScores.get(dongCode) * 0.5
                + commercialScores.get(dongCode) * 0.3
                + parkScores.get(dongCode) * 0.2;
            totals.put(dongCode, total);
        }
        return totals;
    }

    /** 서비스의 정렬 규칙(점수 내림차순, 동 코드 오름차순)과 상위 3개 제한을 재현한다. */
    private List<String> sortTopThree(Map<String, Double> scores) {
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(3)
            .map(Map.Entry::getKey)
            .toList();
    }

    /** SQL의 고정 조건 파라미터와 IN 절의 동 코드 파라미터를 하나의 배열로 합친다. */
    private Object[] combineParameters(Object[] prefixParameters, List<String> dongCodes) {
        Object[] parameters = new Object[prefixParameters.length + dongCodes.size()];
        System.arraycopy(prefixParameters, 0, parameters, 0, prefixParameters.length);
        for (int index = 0; index < dongCodes.size(); index++) {
            parameters[prefixParameters.length + index] = dongCodes.get(index);
        }
        return parameters;
    }
}
