package com.ssafy.prj.model.service.neighborhood.score.school;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;
import com.ssafy.prj.model.mapper.neighborhood.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

import lombok.RequiredArgsConstructor;

/**
 * 운영 중인 학교 수를 기준으로 동별 학교 카테고리 점수를 계산한다.
 */
@Component
@RequiredArgsConstructor
public class SchoolScoreCalculator implements CategoryScoreCalculator {

    private final SchoolMapper schoolMapper;

    @Override
    public SurveyCategory category() {
        return SurveyCategory.SCHOOL;
    }

    @Override
    public Map<String, Double> calculateScores(List<String> dongCodes) {
        /*
         * 학교 점수는 더 이상 "이번 추천 후보 중 몇 등인지"로 계산하지 않는다.
         *
         * 기존 후보군 내부 상대 순위 방식은 추천 후보 안에서만 등수를 매겼기 때문에,
         * 후보가 20~30개 정도만 되어도 상위권 동네가 대부분 90점대에 몰렸다.
         *
         * 학교는 카페/음식점처럼 많을수록 계속 좋아지는 지표라기보다,
         * "동 안에 기본적인 교육시설이 어느 정도 갖춰져 있는가"가 더 중요하다.
         * 그래서 학교 수를 고정 구간으로 나누어 점수를 준다.
         *
         * 이 방식의 장점:
         * 1. 같은 학교 수는 언제나 같은 점수를 받는다.
         * 2. 후보 동네가 바뀌어도 점수 기준이 흔들리지 않는다.
         * 3. 학교가 0개인 동네는 명확하게 0점으로 처리된다.
         */
        Map<String, Double> scoresByDongCode = initializeZeroScores(dongCodes);

        List<NeighborhoodSchoolCount> schoolCounts = schoolMapper.countByDongCodes(dongCodes);
        for (NeighborhoodSchoolCount schoolCount : schoolCounts) {
            scoresByDongCode.put(
                schoolCount.getDongCode(),
                scoreBySchoolCount(schoolCount.getSchoolCount())
            );
        }

        return scoresByDongCode;
    }

    /**
     * 학교 수를 생활환경 해석에 맞는 고정 점수 구간으로 변환한다.
     *
     * <p>학교 수는 법정동 크기와도 관련이 있기 때문에, 단순히 수가 많다고 무한히 높은 가치를
     * 부여하지 않는다. 대신 "없음 / 최소 존재 / 어느 정도 갖춤 / 충분함" 정도의 단계로 본다.</p>
     */
    private double scoreBySchoolCount(Integer schoolCount) {
        int count = schoolCount == null ? 0 : schoolCount;
        if (count <= 0) {
            return 0.0;
        }
        if (count == 1) {
            return 50.0;
        }
        if (count == 2) {
            return 70.0;
        }
        if (count <= 4) {
            return 85.0;
        }
        return 100.0;
    }

    /**
     * Mapper가 LEFT JOIN으로 후보 동네를 모두 돌려주는 구조여도,
     * 방어적으로 요청받은 모든 동네를 먼저 0점으로 채워 둔다.
     *
     * <p>이렇게 하면 특정 동네에 schools 데이터가 없거나, 조회 결과에서 누락되더라도 추천 계산은
     * 중단되지 않고 해당 동네의 학교 점수만 0점으로 처리된다.</p>
     */
    private Map<String, Double> initializeZeroScores(List<String> dongCodes) {
        Map<String, Double> scoresByDongCode = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            scoresByDongCode.put(dongCode, 0.0);
        }
        return scoresByDongCode;
    }
}
