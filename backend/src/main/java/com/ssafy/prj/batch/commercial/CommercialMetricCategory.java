package com.ssafy.prj.batch.commercial;

/**
 * 상권 API의 업종 대분류 코드와 우리 서비스의 추천 카테고리를 연결하는 기준표다.
 *
 * <p>공공데이터 API에는 {@code I2}, {@code R1}, {@code Q1}처럼 대분류 코드가 전달된다.
 * 식료품 소매처럼 더 좁은 범위가 필요한 경우에는 {@code G205} 같은 중분류 코드도 함께 전달한다.
 *
 * <p>추천 DB와 점수 계산에서는 의미가 드러나는 문자열을 사용한다. 이 enum 한 곳에 두 값을
 * 추천 DB와 점수 계산에서는 의미가 드러나는 문자열을 사용한다. 이 enum 한 곳에 두 값을
 * 함께 정의하면, 새 상권 카테고리를 추가할 때 API 요청 코드와 DB 저장 코드를 따로 수정하다가
 * 서로 다르게 만드는 실수를 막을 수 있다.</p>
 */
public enum CommercialMetricCategory {

    /** 소상공인시장진흥공단 대분류 I2: 카페·외식 우선순위에 사용한다. */
    CAFE_RESTAURANT("CAFE_RESTAURANT", "I2", null),

    /** 소상공인시장진흥공단 대분류 R1: 운동·문화 우선순위에 사용한다. */
    CULTURE_SPORTS("CULTURE_SPORTS", "R1", null),

    /** 소상공인시장진흥공단 대분류 Q1: 의료 우선순위에 사용한다. */
    MEDICAL("MEDICAL", "Q1", null),

    /** 소매(G2) 중 식료품 소매(G205). 장보기·생활편의 점수의 첫 번째 원천 지표다. */
    SHOPPING_GROCERY("SHOPPING_GROCERY", "G2", "G205"),

    /** 소매(G2) 중 종합 소매(G204). 장보기·생활편의 점수의 두 번째 원천 지표다. */
    SHOPPING_GENERAL("SHOPPING_GENERAL", "G2", "G204"),

    /** 소매(G2) 중 애완용품(G220). 반려인 점수에서 별도 가중치를 적용하기 위해 분리 저장한다. */
    PET_SUPPLIES("PET_SUPPLIES", "G2", "G220"),

    /** 수리·개인(M1) 중 동물병원(M111). 반려인 점수에서 애완용품과 분리 저장한다. */
    ANIMAL_HOSPITAL("ANIMAL_HOSPITAL", "M1", "M111");

    /** dong_commercial_metrics.category에 저장할 값이다. */
    private final String databaseCategory;

    /** 상권 API 요청의 indsLclsCd 파라미터에 넣을 대분류 코드다. */
    private final String industryCode;

    /**
     * API 요청의 indsMclsCd 값이다. 대분류 전체를 수집하는 카테고리는 null이며,
     * 이 경우 요청 URL에 indsMclsCd 파라미터 자체를 넣지 않는다.
     */
    private final String mediumIndustryCode;

    CommercialMetricCategory(String databaseCategory, String industryCode, String mediumIndustryCode) {
        this.databaseCategory = databaseCategory;
        this.industryCode = industryCode;
        this.mediumIndustryCode = mediumIndustryCode;
    }

    public String getDatabaseCategory() {
        return databaseCategory;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public String getMediumIndustryCode() {
        return mediumIndustryCode;
    }
}
