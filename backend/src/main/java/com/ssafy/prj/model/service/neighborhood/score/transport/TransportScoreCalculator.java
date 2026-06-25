package com.ssafy.prj.model.service.neighborhood.score.transport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodBusStopCount;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSubwayCount;
import com.ssafy.prj.model.mapper.neighborhood.TransportMetricMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;
import com.ssafy.prj.model.service.neighborhood.score.PercentileScorePolicy;

/**
 * 교통 카테고리 점수를 계산한다.
 *
 * <p>교통은 버스 정류장과 지하철을 함께 본다. 서울 생활권에서는 지하철 접근성이 더 크게 체감되므로
 * 지하철 점수 65%, 버스 정류장 점수 35%로 가중합한다.</p>
 *
 * <p>지하철은 단순 row 수를 그대로 쓰지 않고, 물리적 역 개수와 환승 노선 보너스를 합친
 * {@code subwayIndex = 역 개수 × 2 + 추가 노선 수}를 기준으로 점수화한다.</p>
 */
@Component
public class TransportScoreCalculator implements CategoryScoreCalculator {

    private static final double SUBWAY_WEIGHT = 0.65;
    private static final double BUS_STOP_WEIGHT = 0.35;
    private static final double MINIMUM_POSITIVE_SCORE = 20.0;

    private final TransportMetricMapper transportMetricMapper;
    private final PercentileScorePolicy percentileScorePolicy;

    public TransportScoreCalculator(
        TransportMetricMapper transportMetricMapper,
        PercentileScorePolicy percentileScorePolicy
    ) {
        this.transportMetricMapper = transportMetricMapper;
        this.percentileScorePolicy = percentileScorePolicy;
    }

    @Override
    public SurveyCategory category() {
        return SurveyCategory.TRANSPORT;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        /*
         * 1. 후보 동네와 서울 전체의 버스 정류장 수를 각각 조회한다.
         * 2. 후보 동네와 서울 전체의 지하철 index를 각각 조회한다.
         * 3. 두 지표를 서울 전체 기준 백분위 점수로 변환한다.
         * 4. 지하철 65%, 버스 35%로 최종 교통 점수를 만든다.
         */
        Map<String, Double> busStopScores = percentileScorePolicy.calculateScores(
            toBusStopCountMap(transportMetricMapper.countBusStopsByDongCodes(dongCodes), dongCodes),
            toBusStopCountMap(transportMetricMapper.countAllSeoulBusStops(), null),
            MINIMUM_POSITIVE_SCORE
        );
        Map<String, Double> subwayScores = percentileScorePolicy.calculateScores(
            toSubwayIndexMap(transportMetricMapper.countSubwaysByDongCodes(dongCodes), dongCodes),
            toSubwayIndexMap(transportMetricMapper.countAllSeoulSubways(), null),
            MINIMUM_POSITIVE_SCORE
        );

        Map<String, Double> transportScores = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            double score = subwayScores.getOrDefault(dongCode, 0.0) * SUBWAY_WEIGHT
                + busStopScores.getOrDefault(dongCode, 0.0) * BUS_STOP_WEIGHT;
            transportScores.put(dongCode, score);
        }
        return transportScores;
    }

    /** 버스 정류장 DTO 목록을 점수 정책이 읽기 쉬운 dongCode → busStopCount Map으로 바꾼다. */
    private Map<String, Integer> toBusStopCountMap(
        List<NeighborhoodBusStopCount> counts,
        List<String> defaultDongCodes
    ) {
        Map<String, Integer> result = initializeCounts(defaultDongCodes);
        for (NeighborhoodBusStopCount count : counts) {
            result.put(count.getDongCode(), count.getBusStopCount());
        }
        return result;
    }

    /** 지하철 DTO 목록을 점수 정책이 읽기 쉬운 dongCode → subwayIndex Map으로 바꾼다. */
    private Map<String, Integer> toSubwayIndexMap(
        List<NeighborhoodSubwayCount> counts,
        List<String> defaultDongCodes
    ) {
        Map<String, Integer> result = initializeCounts(defaultDongCodes);
        for (NeighborhoodSubwayCount count : counts) {
            result.put(count.getDongCode(), count.getSubwayIndex());
        }
        return result;
    }

    /**
     * 후보 동네 목록이 주어진 경우에는 누락된 동네도 0점으로 계산되도록 먼저 0으로 채운다.
     * 서울 전체 기준 목록은 mapper가 전체 법정동을 반환하므로 별도 초기화가 필요 없다.
     */
    private Map<String, Integer> initializeCounts(List<String> defaultDongCodes) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (defaultDongCodes == null) {
            return result;
        }
        for (String dongCode : defaultDongCodes) {
            result.put(dongCode, 0);
        }
        return result;
    }
}
