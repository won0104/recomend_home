package com.ssafy.prj.model.mapper.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodParkCount;

/**
 * 로컬 DB에 적재한 parks와 dongcodes의 조인 결과를 검증한다.
 * 실행: mvn test -DrunDbIntegrationTests=true
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class ParkMapperIntegrationTest {

    @Autowired
    private ParkMapper parkMapper;

    @Test
    void 동_코드로_조회하면_공원_수와_동_이름을_반환한다() {
        List<String> dongCodes = List.of("4691036021", "4691033022", "4682025036");

        List<NeighborhoodParkCount> result = parkMapper.countByDongCodes(dongCodes);
        Map<String, NeighborhoodParkCount> parksByDongCode = result.stream()
            .collect(Collectors.toMap(NeighborhoodParkCount::getDongCode, item -> item));

        assertThat(parksByDongCode).containsOnlyKeys(dongCodes.toArray(String[]::new));
        assertThat(parksByDongCode.values())
            .allSatisfy(item -> {
                assertThat(item.getDongName()).isNotBlank();
                assertThat(item.getParkCount()).isPositive();
            });
    }
}
