package com.ssafy.prj.model.mapper.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;

/**
 * 로컬 DB에 적재한 dongcodes와 schools의 조인 결과를 검증한다.
 * 실행: mvn test -DrunDbIntegrationTests=true
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class SchoolMapperIntegrationTest {

    @Autowired
    private SchoolMapper schoolMapper;

    @ParameterizedTest(name = "dongCode={0} returns {1} operating schools")
    @CsvSource({
        "4111000000, 205",
        "4146000000, 193"
    })
    void 법정동_코드로_조회하면_기대하는_운영_학교_수를_반환한다(
        String dongCode,
        int expectedSchoolCount
    ) {
        int actualSchoolCount = schoolMapper.countByDongCode(dongCode);

        assertThat(actualSchoolCount).isEqualTo(expectedSchoolCount);
    }

    @Test
    void 동_코드로_조회하면_올바른_동_이름을_반환한다() {
        List<String> dongCodes = List.of("1111010100", "1121510300", "1135010300");

        List<NeighborhoodSchoolCount> result = schoolMapper.countByDongCodes(dongCodes);
        Map<String, String> dongNamesByCode = result.stream()
            .collect(Collectors.toMap(
                NeighborhoodSchoolCount::getDongCode,
                NeighborhoodSchoolCount::getDongName
            ));

        assertThat(dongNamesByCode).containsEntry("1111010100", "청운동");
        assertThat(dongNamesByCode).containsEntry("1121510300", "구의동");
        assertThat(dongNamesByCode).containsEntry("1135010300", "공릉동");
    }
}
