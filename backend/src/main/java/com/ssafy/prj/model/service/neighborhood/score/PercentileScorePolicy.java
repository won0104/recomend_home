package com.ssafy.prj.model.service.neighborhood.score;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 서울 전체 법정동 분포를 기준으로 후보 동네의 시설 수를 백분위 점수로 바꾼다.
 *
 * <p>후보군 내부 순위 방식은 추천 후보가 20~30개일 때 상위권 동네들이 대부분 90점대에 몰리는 문제가 있었다.
 * 이 정책은 후보군이 아니라 서울 전체 법정동의 시설 수 분포를 기준으로 삼기 때문에, 같은 동네는 추천 반경이
 * 달라져도 비교적 안정적인 점수를 받는다.</p>
 *
 * <p>점수 계산 원칙은 다음과 같다.</p>
 *
 * <ul>
 *   <li>시설 수가 0개면 무조건 0점이다.</li>
 *   <li>시설 수가 1개 이상이면 {@code minimumPositiveScore}부터 시작한다.</li>
 *   <li>서울 전체에서 자신보다 시설 수가 적은 동네의 비율이 높을수록 100점에 가까워진다.</li>
 *   <li>같은 시설 수를 가진 동네는 같은 점수를 받는다.</li>
 * </ul>
 */
@Component
public class PercentileScorePolicy {

    /**
     * 백분위 점수를 살짝 보수적으로 눌러 주는 지수값이다.
     *
     * <p>선형 백분위 점수는 서울 상위권 동네들이 대부분 98점대에 몰리는 문제가 있었다.
     * {@code percentile^1.8}을 적용하면 중상위권 점수는 내려가고, 정말 최상위권만 100점에 가까워진다.</p>
     */
    private static final double PERCENTILE_CURVE_EXPONENT = 1.8;

    /**
     * 후보 동네의 count를 서울 전체 count 분포 기준 백분위 점수로 변환한다.
     *
     * @param targetCountsByDongCode 이번 추천 후보 동네들의 시설 수
     * @param baselineCountsByDongCode 서울 전체 법정동의 시설 수
     * @param minimumPositiveScore 시설이 1개 이상 있을 때 받을 수 있는 최소 점수
     * @return 후보 동네별 0~100점
     */
    public Map<String, Double> calculateScores(
        Map<String, Integer> targetCountsByDongCode,
        Map<String, Integer> baselineCountsByDongCode,
        double minimumPositiveScore
    ) {
        Map<String, Double> scoresByDongCode = new LinkedHashMap<>();
        List<Integer> positiveBaselineCounts = baselineCountsByDongCode.values().stream()
            .map(count -> count == null ? 0 : count)
            .filter(count -> count > 0)
            .sorted()
            .toList();

        for (Map.Entry<String, Integer> entry : targetCountsByDongCode.entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue();
            scoresByDongCode.put(
                entry.getKey(),
                calculateScore(count, positiveBaselineCounts, minimumPositiveScore)
            );
        }
        return scoresByDongCode;
    }

    /**
     * 시설 수 하나를 백분위 기반 점수로 바꾼다.
     *
     * <p>양수 데이터가 1개뿐이면 그 동네는 비교 가능한 양수 집단 안에서 최고값이므로 100점으로 본다.
     * 양수 데이터가 여러 개이면 "나보다 작은 count의 개수 / 양수 count 개수 - 1"로 백분위를 계산한다.</p>
     */
    private double calculateScore(
        int count,
        List<Integer> positiveBaselineCounts,
        double minimumPositiveScore
    ) {
        if (count <= 0 || positiveBaselineCounts.isEmpty()) {
            return 0.0;
        }
        if (positiveBaselineCounts.size() == 1) {
            return 100.0;
        }

        long lowerCount = positiveBaselineCounts.stream()
            .filter(baselineCount -> baselineCount < count)
            .count();
        double percentile = lowerCount / (double) (positiveBaselineCounts.size() - 1);
        double adjustedPercentile = Math.pow(percentile, PERCENTILE_CURVE_EXPONENT);
        double boundedMinimumScore = Math.max(0.0, Math.min(100.0, minimumPositiveScore));

        return boundedMinimumScore + adjustedPercentile * (100.0 - boundedMinimumScore);
    }
}
