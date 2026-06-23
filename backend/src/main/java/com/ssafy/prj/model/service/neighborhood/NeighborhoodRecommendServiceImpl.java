package com.ssafy.prj.model.service.neighborhood;

import java.util.HashMap;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.dto.neighborhood.CategoryScore;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodCommercialCount;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.mapper.neighborhood.NeighborhoodRecommendMapper;
import com.ssafy.prj.model.mapper.neighborhood.ParkMapper;
import com.ssafy.prj.model.mapper.neighborhood.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

import lombok.RequiredArgsConstructor;

/**
 * 프론트엔드의 동네 추천 요청을 실제 점수 계산까지 연결하는 서비스다.
 *
 * <h2>처리 순서</h2>
 * <ol>
 *   <li>출발 위치·반경·예산으로 후보 법정동을 먼저 찾는다.</li>
 *   <li>프론트가 보낸 우선순위 문자열을 {@link SurveyCategory} enum으로 변환한다.</li>
 *   <li>학교·공원·상권 Calculator가 후보 동의 점수를 계산해 상위 3개를 고른다.</li>
 *   <li>선정된 코드에 dongcodes의 지역명을 붙여 프론트에 반환하고 서버 콘솔에도 출력한다.</li>
 * </ol>
 *
 * <p>이 서비스는 외부 상권 API를 호출하지 않는다. 상권 데이터는 사전에 실행한 배치 결과인
 * {@code dong_commercial_metrics}만 읽는다.</p>
 */
@Service
@RequiredArgsConstructor
public class NeighborhoodRecommendServiceImpl implements NeighborhoodRecommendService {

    /** 1·2·3순위 생활 조건의 점수 반영 비율이다. */
    private static final List<Double> PRIORITY_WEIGHTS = List.of(0.5, 0.3, 0.2);
    /** 화면에는 최종 상위 세 동네만 보여준다. */
    private static final int RECOMMENDATION_LIMIT = 3;

    private final NeighborhoodRecommendMapper neighborhoodRecommendMapper;
    /** 학교·공원·상권 등 카테고리별 원천 점수를 계산하는 구현체 목록이다. */
    private final List<CategoryScoreCalculator> scoreCalculators;
    private final SchoolMapper schoolMapper;
    private final ParkMapper parkMapper;
    private final CommercialMetricMapper commercialMetricMapper;

    @Override
    @Transactional(readOnly = true)
    public NeighborhoodRecommendResponse recommend(NeighborhoodRecommendRequest request) {
        validate(request);

        // 1. 주거 조건으로 먼저 후보 법정동 목록을 좁힌다.
        List<String> legalDongCodes = neighborhoodRecommendMapper.findLegalDongCodes(request);

        // 2. "CAFE", "PARK" 같은 HTTP 문자열을 점수 계산기가 이해하는 enum으로 변환한다.
        List<SurveyCategory> priorities = toSurveyCategories(request.getPriorities());

        NeighborhoodRecommendResponse response = new NeighborhoodRecommendResponse();
        response.setLegalDongCodes(legalDongCodes);
        response.setTotalCount(legalDongCodes.size());
        response.setPriorities(priorities.stream().map(Enum::name).toList());

        // 후보가 없으면 점수 계산 서비스에 빈 목록을 전달하지 않는다.
        // 이 경우 프론트는 recommendations: []를 받아 "조건에 맞는 동이 없음"을 표현할 수 있다.
        if (legalDongCodes.isEmpty()) {
            response.setRecommendations(List.of());
            System.out.println("[NeighborhoodRecommendation] 후보 동이 없어 추천 결과가 없습니다.");
            return response;
        }

        // 3. 선택 순서(1·2·3순위)를 반영해 상위 3개 동 코드와 점수를 계산한다.
        NeighborhoodRecommendationResult recommendationResult = recommendWithDetails(
            legalDongCodes,
            priorities
        );
        List<RecommendedNeighborhood> recommendations = recommendationResult.recommendations();

        // 4. 점수 결과에는 코드만 있으므로, dongcodes에서 사람이 읽을 수 있는 지역명을 보강한다.
        enrichNeighborhoodNames(recommendations);
        // 5. 카테고리별 점수 내역을 각 추천 결과에 추가해 프론트에서 바로 사용할 수 있게 한다.
        enrichCategoryScores(
            recommendations,
            recommendationResult.scoresByCategory(),
            recommendationResult.priorityWeights(),
            priorities
        );
        response.setRecommendations(recommendations);

        // 6. 화면 개발 중 즉시 확인할 수 있도록 최종 3개를 서버 실행 콘솔에 출력한다.
        printRecommendations(
            recommendations,
            recommendationResult.scoresByCategory(),
            recommendationResult.priorityWeights(),
            priorities
        );
        return response;
    }

    /**
     * 후보 동의 카테고리 점수를 합산해 추천 결과와 점수 설명 데이터를 함께 만든다.
     *
     * <p>후보 조회·점수 합산·응답 조립은 하나의 추천 요청 흐름이므로 이 서비스 안에서 순서대로 처리한다.</p>
     */
    private NeighborhoodRecommendationResult recommendWithDetails(
        List<String> dongCodes,
        List<SurveyCategory> priorities
    ) {
        Map<SurveyCategory, Map<String, Double>> scoresByCategory = new EnumMap<>(SurveyCategory.class);
        for (SurveyCategory category : priorities) {
            scoresByCategory.put(category, getCategoryScores(category, dongCodes));
        }

        Map<String, Double> totalScores = new LinkedHashMap<>();
        for (String dongCode : dongCodes) {
            double totalScore = 0.0;
            for (int index = 0; index < priorities.size(); index++) {
                SurveyCategory category = priorities.get(index);
                double categoryScore = scoresByCategory.get(category).getOrDefault(dongCode, 0.0);
                totalScore += categoryScore * PRIORITY_WEIGHTS.get(index);
            }
            totalScores.put(dongCode, totalScore);
        }

        List<Map.Entry<String, Double>> sortedScores = totalScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(java.util.Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(RECOMMENDATION_LIMIT)
            .toList();
        List<RecommendedNeighborhood> recommendations = java.util.stream.IntStream.range(0, sortedScores.size())
            .mapToObj(index -> {
                Map.Entry<String, Double> score = sortedScores.get(index);
                RecommendedNeighborhood recommendation = new RecommendedNeighborhood();
                recommendation.setLawdCd(score.getKey());
                recommendation.setRank(index + 1);
                recommendation.setScore(score.getValue());
                return recommendation;
            })
            .toList();

        return new NeighborhoodRecommendationResult(recommendations, scoresByCategory, PRIORITY_WEIGHTS);
    }

    /** 선택한 카테고리를 담당하는 Calculator를 찾아, 후보 동별 상대 점수를 계산한다. */
    private Map<String, Double> getCategoryScores(SurveyCategory category, List<String> dongCodes) {
        return scoreCalculators.stream()
            .filter(calculator -> calculator.category() == category)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 카테고리입니다: " + category))
            .calculateScores(dongCodes);
    }

    /**
     * 요청 문자열을 enum으로 바꾸고, 이전 프론트의 임시 식별자도 호환 처리한다.
     * 현재 프론트는 enum 이름을 그대로 보내지만 HEALTH/MARKET을 보내던 기존 화면도 깨지지 않는다.
     */
    private List<SurveyCategory> toSurveyCategories(List<String> priorities) {
        List<String> normalized = priorities == null ? List.of() : priorities.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(priority -> !priority.isEmpty())
            .map(priority -> priority.toUpperCase(Locale.ROOT))
            .map(this::normalizeLegacyPriority)
            .toList();

        if (normalized.size() != 3) {
            throw new IllegalArgumentException("우선순위는 중복 없이 정확히 3개를 선택해야 합니다.");
        }
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("우선순위 카테고리는 중복해서 선택할 수 없습니다.");
        }

        try {
            return normalized.stream()
                .map(SurveyCategory::valueOf)
                .toList();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 생활 우선순위가 포함되어 있습니다: " + normalized);
        }
    }

    /** 과거 화면의 임시 id를 현재 설문 카테고리 이름으로만 치환한다. */
    private String normalizeLegacyPriority(String priority) {
        return switch (priority) {
            case "HEALTH" -> SurveyCategory.CULTURE.name();
            case "MARKET" -> SurveyCategory.SHOPPING.name();
            default -> priority;
        };
    }

    /** 코드로 조회한 지역명 결과를 점수·순위 객체에 합친다. */
    private void enrichNeighborhoodNames(List<RecommendedNeighborhood> recommendations) {
        if (recommendations.isEmpty()) {
            return;
        }

        List<String> dongCodes = recommendations.stream()
            .map(RecommendedNeighborhood::getLawdCd)
            .toList();
        Map<String, RecommendedNeighborhood> namesByDongCode = new HashMap<>();
        for (RecommendedNeighborhood neighborhood : neighborhoodRecommendMapper.findNeighborhoodsByDongCodes(
            dongCodes
        )) {
            namesByDongCode.put(neighborhood.getLawdCd(), neighborhood);
        }

        for (RecommendedNeighborhood recommendation : recommendations) {
            RecommendedNeighborhood nameSource = namesByDongCode.get(recommendation.getLawdCd());
            if (nameSource == null) {
                continue;
            }
            recommendation.setSidoName(nameSource.getSidoName());
            recommendation.setSigunguName(nameSource.getSigunguName());
            recommendation.setDongName(nameSource.getDongName());
        }
    }

    /** 카테고리별 점수 내역을 각 추천 결과 객체에 채워 프론트에서 바로 사용할 수 있게 한다. */
    private void enrichCategoryScores(
        List<RecommendedNeighborhood> recommendations,
        Map<SurveyCategory, Map<String, Double>> scoresByCategory,
        List<Double> priorityWeights,
        List<SurveyCategory> priorities
    ) {
        for (RecommendedNeighborhood recommendation : recommendations) {
            String dongCode = recommendation.getLawdCd();
            List<CategoryScore> scores = new java.util.ArrayList<>();
            for (int i = 0; i < priorities.size(); i++) {
                SurveyCategory category = priorities.get(i);
                double score = scoresByCategory.getOrDefault(category, Map.of())
                    .getOrDefault(dongCode, 0.0);
                double weight = priorityWeights.get(i);
                scores.add(new CategoryScore(category.name(), score, weight, score * weight));
            }
            recommendation.setCategoryScores(scores);
        }
    }

    /**
     * 최종 추천 3개의 순위·총점·카테고리별 점수·원천 데이터 개수를 서버 콘솔에 출력한다.
     *
     * <p>점수는 후보 동 전체를 비교한 상대 점수이고, count는 해당 동이 실제로 가진 원천 데이터
     * 개수다. 둘을 함께 출력해야 "업소 수는 적지만 다른 우선순위 점수가 높아 추천된 경우"도
     * 쉽게 확인할 수 있다.</p>
     */
    private void printRecommendations(
        List<RecommendedNeighborhood> recommendations,
        Map<SurveyCategory, Map<String, Double>> scoresByCategory,
        List<Double> priorityWeights,
        List<SurveyCategory> priorities
    ) {
        if (recommendations.isEmpty()) {
            return;
        }

        // 상위 3개만 다시 조회한다. 후보 전체를 재조회하지 않아 로그 기능이 추천 성능에 영향을 주지 않는다.
        List<String> recommendedCodes = recommendations.stream()
            .map(RecommendedNeighborhood::getLawdCd)
            .toList();
        Map<String, Integer> schoolCounts = toSchoolCountMap(schoolMapper.countByDongCodes(recommendedCodes));
        Map<String, Integer> parkCounts = toParkCountMap(parkMapper.countByDongCodes(recommendedCodes));

        // 상권 지표는 사용자가 실제로 고른 카테고리에 필요한 것만 조회한다.
        Map<String, Map<String, Integer>> commercialCountsByMetric = new HashMap<>();
        for (SurveyCategory priority : priorities) {
            for (String metricCategory : requiredCommercialMetrics(priority)) {
                commercialCountsByMetric.computeIfAbsent(metricCategory,
                    category -> toCommercialCountMap(commercialMetricMapper.countByDongCodes(
                        recommendedCodes,
                        category
                    ))
                );
            }
        }

        for (RecommendedNeighborhood recommendation : recommendations) {
            String fullName = String.join(" ", List.of(
                nullToEmpty(recommendation.getSidoName()),
                nullToEmpty(recommendation.getSigunguName()),
                nullToEmpty(recommendation.getDongName())
            )).trim().replaceAll(" +", " ");
            System.out.println("[NeighborhoodRecommendation] rank=" + recommendation.getRank()
                + ", dongCode=" + recommendation.getLawdCd()
                + ", name=" + fullName
                + ", totalScore=" + String.format(Locale.ROOT, "%.2f", recommendation.getScore()));

            // 사용자가 선택한 순서대로 각 카테고리의 점수와 해당 원천 개수를 한 줄씩 출력한다.
            for (int index = 0; index < priorities.size(); index++) {
                SurveyCategory priority = priorities.get(index);
                double categoryScore = scoresByCategory.getOrDefault(priority, Map.of())
                    .getOrDefault(recommendation.getLawdCd(), 0.0);
                // 가중치는 점수 계산 서비스가 실제 사용한 값을 전달받아, 로그와 계산 규칙이 어긋나지 않는다.
                double weight = priorityWeights.get(index);
                double weightedScore = categoryScore * weight;
                System.out.println("  - priority=" + (index + 1)
                    + ", category=" + priority.name()
                    + ", score=" + String.format(Locale.ROOT, "%.2f", categoryScore)
                    + ", weight=" + String.format(Locale.ROOT, "%.2f", weight)
                    + ", contribution=" + String.format(Locale.ROOT, "%.2f", weightedScore)
                    + ", " + describeCounts(
                        priority,
                        recommendation.getLawdCd(),
                        schoolCounts,
                        parkCounts,
                        commercialCountsByMetric
                    ));
            }
        }
    }

    /**
     * 각 화면 카테고리가 로그에 표시해야 할 상권 원천 지표를 정의한다.
     * 공원·학교는 별도 테이블을 사용하므로 이 목록에 넣지 않는다.
     */
    private List<String> requiredCommercialMetrics(SurveyCategory category) {
        return switch (category) {
            case CAFE -> List.of("CAFE_RESTAURANT");
            case CULTURE -> List.of("CULTURE_SPORTS");
            case SHOPPING -> List.of("SHOPPING_GROCERY", "SHOPPING_GENERAL");
            case MEDICAL -> List.of("MEDICAL");
            case PET -> List.of("PET_SUPPLIES", "ANIMAL_HOSPITAL");
            case SCHOOL, PARK -> List.of();
        };
    }

    /** 카테고리별 로그에 사람이 바로 이해할 수 있는 원천 데이터 개수를 구성한다. */
    private String describeCounts(
        SurveyCategory category,
        String dongCode,
        Map<String, Integer> schoolCounts,
        Map<String, Integer> parkCounts,
        Map<String, Map<String, Integer>> commercialCountsByMetric
    ) {
        return switch (category) {
            case SCHOOL -> "schoolCount=" + schoolCounts.getOrDefault(dongCode, 0);
            case PARK -> "parkCount=" + parkCounts.getOrDefault(dongCode, 0);
            case CAFE -> "cafeRestaurantCount=" + metricCount(
                commercialCountsByMetric, "CAFE_RESTAURANT", dongCode);
            case CULTURE -> "cultureSportsCount=" + metricCount(
                commercialCountsByMetric, "CULTURE_SPORTS", dongCode);
            case MEDICAL -> "medicalCount=" + metricCount(commercialCountsByMetric, "MEDICAL", dongCode);
            case SHOPPING -> {
                int groceryCount = metricCount(commercialCountsByMetric, "SHOPPING_GROCERY", dongCode);
                int generalCount = metricCount(commercialCountsByMetric, "SHOPPING_GENERAL", dongCode);
                yield "groceryCount=" + groceryCount
                    + ", generalRetailCount=" + generalCount
                    + ", shoppingTotalCount=" + (groceryCount + generalCount);
            }
            case PET -> {
                int petSuppliesCount = metricCount(commercialCountsByMetric, "PET_SUPPLIES", dongCode);
                int animalHospitalCount = metricCount(commercialCountsByMetric, "ANIMAL_HOSPITAL", dongCode);
                yield "petSuppliesCount=" + petSuppliesCount
                    + ", parkCount=" + parkCounts.getOrDefault(dongCode, 0)
                    + ", animalHospitalCount=" + animalHospitalCount;
            }
        };
    }

    /** 특정 상권 원천 지표의 동별 개수를 안전하게 꺼낸다. */
    private int metricCount(
        Map<String, Map<String, Integer>> commercialCountsByMetric,
        String metricCategory,
        String dongCode
    ) {
        return commercialCountsByMetric.getOrDefault(metricCategory, Map.of()).getOrDefault(dongCode, 0);
    }

    /** SchoolMapper의 DTO 목록을 코드 -> 학교 수 형태로 바꾼다. */
    private Map<String, Integer> toSchoolCountMap(List<NeighborhoodSchoolCount> counts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (counts != null) {
            for (NeighborhoodSchoolCount count : counts) {
                result.put(count.getDongCode(), count.getSchoolCount());
            }
        }
        return result;
    }

    /** ParkMapper의 DTO 목록을 코드 -> 공원 수 형태로 바꾼다. */
    private Map<String, Integer> toParkCountMap(List<NeighborhoodParkCount> counts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (counts != null) {
            for (NeighborhoodParkCount count : counts) {
                result.put(count.getDongCode(), count.getParkCount());
            }
        }
        return result;
    }

    /** CommercialMetricMapper의 DTO 목록을 코드 -> 업소 수 형태로 바꾼다. */
    private Map<String, Integer> toCommercialCountMap(List<NeighborhoodCommercialCount> counts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (counts != null) {
            for (NeighborhoodCommercialCount count : counts) {
                result.put(count.getDongCode(), count.getBusinessCount());
            }
        }
        return result;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 위치·반경·예산 값 자체가 유효한지 후보 조회 전에 검사한다. */
    private void validate(NeighborhoodRecommendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("추천 조건이 없습니다.");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("기준 위치의 위도와 경도가 필요합니다.");
        }
        if (request.getRadiusKm() == null || request.getRadiusKm() <= 0) {
            throw new IllegalArgumentException("반경 범위는 0보다 커야 합니다.");
        }
        validatePositiveBudget("매매가 최대", request.getDealAmountMax());
        validatePositiveBudget("보증금 최대", request.getDepositMax());
        validatePositiveBudget("월세 최대", request.getMonthlyRentMax());

        String dealType = request.getDealType() == null ? "매매" : request.getDealType().trim();
        if (!List.of("매매", "전세", "월세").contains(dealType)) {
            throw new IllegalArgumentException("거래 유형은 매매, 전세, 월세 중 하나여야 합니다.");
        }
        request.setDealType(dealType);
    }

    private void validatePositiveBudget(String label, Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(label + "은(는) 0 이상이어야 합니다.");
        }
    }

    private void validateRange(String label, Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(label + " 최소값은 최대값보다 클 수 없습니다.");
        }
    }
}
