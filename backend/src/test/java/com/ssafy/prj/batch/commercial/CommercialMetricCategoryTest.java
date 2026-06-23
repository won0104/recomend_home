package com.ssafy.prj.batch.commercial;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 상권 배치의 업종 코드 기준표를 검증한다.
 *
 * <p>이 테스트는 외부 API를 호출하지 않는다. API 요청에 들어갈 대분류·중분류 코드와 DB category가
 * 기획한 값으로 연결되어 있는지를 빠르게 확인한다. 업종 코드를 수정할 때 가장 먼저 깨져야 하는
 * 안전망이다.</p>
 */
class CommercialMetricCategoryTest {

    @Test
    void 장보기와_반려인_원천_지표는_대분류와_중분류_코드를_각각_가진다() {
        // 장보기·생활편의: 같은 소매(G2) 안에서도 두 중분류를 별도로 수집해야 나중에 합산할 수 있다.
        assertCategory(
            CommercialMetricCategory.SHOPPING_GROCERY,
            "SHOPPING_GROCERY",
            "G2",
            "G205"
        );
        assertCategory(
            CommercialMetricCategory.SHOPPING_GENERAL,
            "SHOPPING_GENERAL",
            "G2",
            "G204"
        );

        // 반려인: 서로 다른 가중치를 적용할 예정이므로 애완용품과 동물병원을 절대 합쳐 저장하지 않는다.
        assertCategory(
            CommercialMetricCategory.PET_SUPPLIES,
            "PET_SUPPLIES",
            "G2",
            "G220"
        );
        assertCategory(
            CommercialMetricCategory.ANIMAL_HOSPITAL,
            "ANIMAL_HOSPITAL",
            "M1",
            "M111"
        );
    }

    @Test
    void 기존_대분류_카테고리는_중분류_필터_없이_전체를_수집한다() {
        // I2/R1/Q1은 대분류 전체를 수집해야 하므로 indsMclsCd가 null이어야 한다.
        assertThat(CommercialMetricCategory.CAFE_RESTAURANT.getMediumIndustryCode()).isNull();
        assertThat(CommercialMetricCategory.CULTURE_SPORTS.getMediumIndustryCode()).isNull();
        assertThat(CommercialMetricCategory.MEDICAL.getMediumIndustryCode()).isNull();
    }

    /** 한 enum 항목의 DB 저장값과 API 요청 파라미터를 함께 검증하는 보조 메서드다. */
    private void assertCategory(
        CommercialMetricCategory category,
        String databaseCategory,
        String industryCode,
        String mediumIndustryCode
    ) {
        assertThat(category.getDatabaseCategory()).isEqualTo(databaseCategory);
        assertThat(category.getIndustryCode()).isEqualTo(industryCode);
        assertThat(category.getMediumIndustryCode()).isEqualTo(mediumIndustryCode);
    }
}
