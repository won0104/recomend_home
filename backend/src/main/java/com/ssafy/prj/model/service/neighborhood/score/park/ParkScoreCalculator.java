package com.ssafy.prj.model.service.neighborhood.score.park;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;
import com.ssafy.prj.model.mapper.neighborhood.ParkMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

import lombok.RequiredArgsConstructor;

/** 법정동별 공원 수를 기준으로 공원 카테고리 점수를 계산한다. */
@Component
@RequiredArgsConstructor
public class ParkScoreCalculator implements CategoryScoreCalculator {

    private final ParkMapper parkMapper;

    @Override
    public SurveyCategory category() {
        return SurveyCategory.PARK;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        /*
         * 공원 점수도 후보군 내 상대 순위가 아니라 고정 구간 점수로 계산한다.
         *
         * 공원은 "개수"만으로 완벽히 평가하기 어려운 지표다. 큰 공원 1개와 작은 공원 여러 개의
         * 생활 체감이 다를 수 있는데, 현재 데이터에는 공원 면적이나 접근 시간 정보가 없다.
         *
         * 그래서 세밀한 순위 경쟁보다는, 공원 수가 어느 정도 확보되어 있는지를 단계적으로 본다.
         * 이 방식은 과도하게 90점대가 몰리는 문제를 줄이고, 0개 동네도 명확하게 구분한다.
         */
        Map<String, Double> scoresByDongCode = initializeZeroScores(dongCodes);

        List<NeighborhoodParkCount> parkCounts = parkMapper.countByDongCodes(dongCodes);
        for (NeighborhoodParkCount parkCount : parkCounts) {
            scoresByDongCode.put(
                parkCount.getDongCode(),
                scoreByParkCount(parkCount.getParkCount())
            );
        }

        return scoresByDongCode;
    }

    /**
     * 공원 수를 산책·휴식 환경 해석에 맞는 고정 점수 구간으로 변환한다.
     *
     * <p>공원 면적 데이터가 없는 상태에서는 "공원이 더 많으면 무조건 더 좋다"라고 세밀하게
     * 점수화하기 어렵다. 따라서 1개, 2~3개, 4~6개, 7개 이상처럼 체감 가능한 단계로 나눈다.</p>
     */
    private double scoreByParkCount(Integer parkCount) {
        int count = parkCount == null ? 0 : parkCount;
        if (count <= 0) {
            return 0.0;
        }
        if (count == 1) {
            return 55.0;
        }
        if (count <= 3) {
            return 75.0;
        }
        if (count <= 6) {
            return 90.0;
        }
        return 100.0;
    }

    /**
     * 조회 결과에 특정 동네가 빠져도 추천 계산이 깨지지 않도록,
     * 요청받은 모든 후보 동네를 먼저 0점으로 초기화한다.
     */
    private Map<String, Double> initializeZeroScores(List<String> dongCodes) {
        Map<String, Double> scoresByDongCode = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            scoresByDongCode.put(dongCode, 0.0);
        }
        return scoresByDongCode;
    }
}
