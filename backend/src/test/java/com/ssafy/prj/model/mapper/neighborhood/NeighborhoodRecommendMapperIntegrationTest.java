package com.ssafy.prj.model.mapper.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.mapper.HouseInfoMapper;

/** 매매·전세·월세별 후보 동 SQL이 실제 MySQL에서 모두 실행되는지 검증한다. */
@SpringBootTest
@TestPropertySource(properties = {
        "batch.estate.enabled=false",
        "batch.estate-rent.enabled=false",
        "batch.estate-rent.run-on-startup=false",
        "batch.commercial.enabled=false"
})
class NeighborhoodRecommendMapperIntegrationTest {

    @Autowired
    private NeighborhoodRecommendMapper neighborhoodRecommendMapper;

    @Autowired
    private HouseInfoMapper houseInfoMapper;

    @Test
    void 거래유형별_후보동_쿼리를_실행한다() {
        for (String dealType : List.of("매매", "전세", "월세")) {
            NeighborhoodRecommendRequest request = new NeighborhoodRecommendRequest();
            request.setLatitude(37.5665);
            request.setLongitude(126.9780);
            request.setRadiusKm(50.0);
            request.setDealType(dealType);
            request.setDealAmountMax(100_000);
            request.setDepositMax(100_000);
            request.setMonthlyRentMax(1_000);

            assertThat(neighborhoodRecommendMapper.findLegalDongCodes(request))
                    .as("%s 후보 동 조회 결과", dealType)
                    .isNotNull();

            assertThat(houseInfoMapper.search("11110", null, null, dealType, "아파트",
                    0, 100_000, 0, 100_000, 0, 1_000))
                    .as("%s 상세 매물 조회 결과", dealType)
                    .isNotNull();
        }
    }
}
