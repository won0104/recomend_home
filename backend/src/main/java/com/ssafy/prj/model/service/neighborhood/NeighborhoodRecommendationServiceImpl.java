package com.ssafy.prj.model.service.neighborhood;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.mapper.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.dto.RecommendedNeighborhood;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NeighborhoodRecommendationServiceImpl implements NeighborhoodRecommendationService {

    private static final List<Double> PRIORITY_WEIGHTS = List.of(0.5, 0.3, 0.2);

    private final SchoolMapper schoolMapper;

    @Override
    public List<RecommendedNeighborhood> getRecommendations(
        List<String> dongList,
        List<SurveyCategory> priorities
    ) throws Exception {
        if (priorities == null || priorities.size() != PRIORITY_WEIGHTS.size()) {
            throw new IllegalArgumentException("우선순위 카테고리는 정확히 3개여야 합니다.");
        }
        if (dongList == null || dongList.isEmpty()) {
            throw new IllegalArgumentException("후보 동네 목록은 비어 있을 수 없습니다.");
        }
        
        log.info("상대 평가 기반 동네 추천 시작 - 후보 수: {}개, 우선순위: {}", 
            dongList.size(), priorities);
        
        int candidateCount = dongList.size();
        List<SurveyCategory> selectedCategories = priorities;
        
        // 1단계: 모든 카테고리에 대해 모든 후보 동네의 데이터 수집
        Map<String, Map<SurveyCategory, Integer>> allCategoryData =
            collectAllCategoryData(dongList, selectedCategories);
        log.debug("1단계 완료: 모든 카테고리 데이터 수집");
        
        // 2단계: 각 카테고리별 순위 점수 계산 (상대 평가)
        Map<String, Map<SurveyCategory, Double>> allCategoryScores = 
            calculateCategoryScores(allCategoryData, candidateCount, selectedCategories);
        log.debug("2단계 완료: 카테고리별 순위 점수 계산");
        
        // 3단계: 설문의 우선순위 기반으로 총점 계산 (선택한 3개 카테고리만)
        Map<String, Double> totalScores = calculateTotalScores(
            dongList, 
            allCategoryScores, 
            priorities
        );
        log.debug("3단계 완료: 설문 가중치 적용하여 총점 계산");
        
        // 4단계: 총점 기준으로 정렬하여 상위 3개 반환
        List<RecommendedNeighborhood> recommendations = totalScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))  // 내림차순
            .limit(3)
            .map(entry -> createRecommendedNeighborhood(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        // 순위 설정
        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRank(i + 1);
        }
        
        log.info("최종 추천 - 1위: {}점, 2위: {}점, 3위: {}점",
            recommendations.size() > 0 ? String.format("%.1f", recommendations.get(0).getScore()) : "-",
            recommendations.size() > 1 ? String.format("%.1f", recommendations.get(1).getScore()) : "-",
            recommendations.size() > 2 ? String.format("%.1f", recommendations.get(2).getScore()) : "-"
        );
        
        return recommendations;
    }

    /**
     * 1단계: 모든 후보에 대해 모든 카테고리의 데이터 수집
     */
    private Map<String, Map<SurveyCategory, Integer>> collectAllCategoryData(
        List<String> dongList,
        List<SurveyCategory> selectedCategories
    ) {
        Map<String, Map<SurveyCategory, Integer>> allData = new HashMap<>();
        
        for (String lawdCd : dongList) {
            Map<SurveyCategory, Integer> categoryData = new HashMap<>();
            
            for (SurveyCategory category : selectedCategories) {
                Integer count = getCategoryCount(lawdCd, category);
                categoryData.put(category, count != null ? count : 0);
            }
            
            allData.put(lawdCd, categoryData);
            log.debug("법정동 {} - 데이터 수집: {}", lawdCd, categoryData);
        }
        
        return allData;
    }

    /**
     * 2단계: 각 카테고리별로 순위를 매기고 점수 부여 (상대 평가)
     * 
     * 1등: 100점, 2등: 90점, ..., n등: 100 - (n-1)*10 점
     */
    private Map<String, Map<SurveyCategory, Double>> calculateCategoryScores(
        Map<String, Map<SurveyCategory, Integer>> allCategoryData,
        int candidateCount,
        List<SurveyCategory> selectedCategories
    ) {
        Map<String, Map<SurveyCategory, Double>> allScores = new HashMap<>();
        
        // 각 카테고리별로 순위 산정
        for (SurveyCategory category : selectedCategories) {
            
            // 해당 카테고리에서 모든 후보를 내림차순으로 정렬
            List<Map.Entry<String, Integer>> rankedList = allCategoryData.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    entry.getValue().get(category)
                ))
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))  // 내림차순
                .collect(Collectors.toList());
            
            log.debug("[{}] 순위:", category.getLabel());
            
            // 순위에 따라 점수 부여
            for (int rank = 0; rank < rankedList.size(); rank++) {
                String lawdCd = rankedList.get(rank).getKey();
                Integer count = rankedList.get(rank).getValue();
                
                // 점수 계산: (후보수 - 순위) * 100 / 후보수
                // 예: 10명 중 1등 = (10-0)*100/10 = 100점
                //     10명 중 2등 = (10-1)*100/10 = 90점
                //     10명 중 10등 = (10-9)*100/10 = 10점
                Double score = (candidateCount - rank) * 100.0 / candidateCount;
                
                allScores.computeIfAbsent(lawdCd, k -> new HashMap<>())
                    .put(category, score);
                
                log.debug("  {}위: 법정동 {} (데이터 {}개) -> {} 점",
                    rank + 1, lawdCd, count, String.format("%.1f", score));
            }
        }
        
        return allScores;
    }

    /**
     * 3단계: 설문의 우선순위 기반으로 총점 계산
     * 
     * 설문에서 선택한 3개 카테고리의 점수만 가중치 적용하여 합산
     */
    private Map<String, Double> calculateTotalScores(
        List<String> dongList,
        Map<String, Map<SurveyCategory, Double>> allCategoryScores,
        List<SurveyCategory> priorities
    ) {
        Map<String, Double> totalScores = new HashMap<>();
        
        for (String lawdCd : dongList) {
            Double totalScore = 0.0;
            
            // 설문의 우선순위 3개에 대해서만 계산
            for (int i = 0; i < priorities.size(); i++) {
                SurveyCategory category = priorities.get(i);
                Double weight = PRIORITY_WEIGHTS.get(i);
                Double categoryScore = allCategoryScores.get(lawdCd).get(category);
                
                Double weightedScore = categoryScore * weight;
                totalScore += weightedScore;
                
                log.debug("  법정동 {} - {} ({}순위, 가중치 {}): {} × {} = {} 점",
                    lawdCd, category.getLabel(), i + 1, weight,
                    String.format("%.1f", categoryScore), weight,
                    String.format("%.1f", weightedScore));
            }
            
            totalScores.put(lawdCd, totalScore);
            log.info("법정동 {} - 최종 총점: {} 점", lawdCd, String.format("%.1f", totalScore));
        }
        
        return totalScores;
    }

    /**
     * 법정동의 카테고리별 데이터 개수 조회
     * 
     * TODO: 실제 Mapper 호출로 대체
     */
    private Integer getCategoryCount(String lawdCd, SurveyCategory category) {
        switch (category) {
            case SCHOOL:
                return getSchoolCount(lawdCd);
            case CAFE:
                return getCafeCount(lawdCd);
            case CULTURE:
                return getCultureCount(lawdCd);
            case SHOPPING:
                return getShoppingCount(lawdCd);
            case PARK:
                return getParkCount(lawdCd);
            case MEDICAL:
                return getMedicalCount(lawdCd);
            case PET:
                return getPetCount(lawdCd);
            default:
                return 0;
        }
    }

    /**
     * 각 카테고리별 조회 메서드
     * TODO: 다음과 같이 Mapper 호출로 대체
     * - commercialMapper.countByCafeAndLawdCd(lawdCd)
     * - cultureMapper.countByLawdCd(lawdCd)
     * - shoppingMapper.countByLawdCd(lawdCd)
     * - parkMapper.countByLawdCd(lawdCd)
     * - medicalMapper.countByLawdCd(lawdCd)
     * - petMapper.countByLawdCd(lawdCd)
     */
    
    /**
     * 학교 수 조회 (schoolMapper 연동)
     */
    private Integer getSchoolCount(String lawdCd) {
        try {
            int count = schoolMapper.countByDongCode(lawdCd);
            log.debug("학교 조회 - 법정동 {}: {} 개", lawdCd, count);
            return count;
        } catch (Exception e) {
            log.error("학교 수 조회 실패 - 법정동 {}: {}", lawdCd, e.getMessage());
            return 0;
        }
    }
    
    private Integer getCafeCount(String lawdCd) { return 0; }
    private Integer getCultureCount(String lawdCd) { return 0; }
    private Integer getShoppingCount(String lawdCd) { return 0; }
    private Integer getParkCount(String lawdCd) { return 0; }
    private Integer getMedicalCount(String lawdCd) { return 0; }
    private Integer getPetCount(String lawdCd) { return 0; }

    /**
     * RecommendedNeighborhood 객체 생성
     */
    private RecommendedNeighborhood createRecommendedNeighborhood(String lawdCd, Double score) {
        RecommendedNeighborhood neighborhood = new RecommendedNeighborhood();
        neighborhood.setLawdCd(lawdCd);
        neighborhood.setScore(score);
        
        // TODO: 실제로는 DongCodeMapper를 호출하여 동명, 시군구명, 시도명 조회
        neighborhood.setDongName("임시동");
        neighborhood.setSigunguName("임시시군구");
        neighborhood.setSidoName("임시시도");
        
        return neighborhood;
    }
}
