package com.ssafy.prj.model.service.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ssafy.prj.model.mapper.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.dto.RecommendedNeighborhood;
import com.ssafy.prj.model.service.neighborhood.dto.NeighborhoodSchoolCount;

@ExtendWith(MockitoExtension.class)
class NeighborhoodRecommendationServiceImplTest {

    @Mock
    private SchoolMapper schoolMapper;

    @InjectMocks
    private NeighborhoodRecommendationServiceImpl recommendationService;

    @Test
    void 학교_수에_따라_후보_동네를_내림차순으로_추천한다() throws Exception {
        List<String> dongList = List.of("1111010100", "1111010200", "1111010300");
        List<SurveyCategory> priorities = List.of(
            SurveyCategory.SCHOOL,
            SurveyCategory.SCHOOL,
            SurveyCategory.SCHOOL
        );

        when(schoolMapper.countByDongCode("1111010100")).thenReturn(10);
        when(schoolMapper.countByDongCode("1111010200")).thenReturn(30);
        when(schoolMapper.countByDongCode("1111010300")).thenReturn(20);

        List<RecommendedNeighborhood> result =
            recommendationService.getRecommendations(dongList, priorities);

        assertThat(result).extracting(RecommendedNeighborhood::getLawdCd)
            .containsExactly("1111010200", "1111010300", "1111010100");
        assertThat(result).extracting(RecommendedNeighborhood::getRank)
            .containsExactly(1, 2, 3);
    }
}

/**
 * 실제 로컬 MySQL의 schools, dongcodes 조인 결과를 확인하는 통합 테스트.
 * 실행: mvn test -DrunDbIntegrationTests=true
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "runDbIntegrationTests", matches = "true")
class NeighborhoodRecommendationDatabaseIntegrationTest {

    @Autowired
    private SchoolMapper schoolMapper;

    @Test
    void 동코드별_학교수와_실제_동이름을_함께_조회한다() {
        // schools와 dongcodes에 실제로 존재하는 코드로 필요하면 교체한다.
        List<String> dongCodes = List.of("1111010100", "1111010200", "1111010300");

        List<NeighborhoodSchoolCount> result = schoolMapper.countByDongCodes(dongCodes);

        assertThat(result).isNotEmpty();
        assertThat(result)
            .allSatisfy(item -> {
                assertThat(item.getDongCode()).isIn(dongCodes);
                assertThat(item.getDongName()).isNotBlank();
                assertThat(item.getSchoolCount()).isNotNegative();
            });
    }
}
