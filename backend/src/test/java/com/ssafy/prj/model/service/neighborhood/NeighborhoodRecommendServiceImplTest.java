package com.ssafy.prj.model.service.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;
import com.ssafy.prj.model.mapper.neighborhood.NeighborhoodRecommendMapper;
import com.ssafy.prj.model.mapper.neighborhood.CommercialMetricMapper;
import com.ssafy.prj.model.mapper.neighborhood.ParkMapper;
import com.ssafy.prj.model.mapper.neighborhood.SchoolMapper;
import com.ssafy.prj.model.service.neighborhood.score.CategoryScoreCalculator;

/**
 * 프론트 추천 API가 사용하는 조립 서비스의 흐름을 검증한다.
 *
 * <p>후보 동 DB 조회와 개별 카테고리 점수 계산은 각각 Mock으로 대체한다. 이 테스트는
 * "후보 코드 조회 → 상위 3개 점수 결과 → 지역명 보강 → 응답" 연결이 맞는지에 집중한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class NeighborhoodRecommendServiceImplTest {

    @Mock
    private NeighborhoodRecommendMapper neighborhoodRecommendMapper;

    @Mock private CategoryScoreCalculator cafeScoreCalculator;
    @Mock private CategoryScoreCalculator parkScoreCalculator;
    @Mock private CategoryScoreCalculator petScoreCalculator;

    /** 최종 로그에 학교 원천 개수를 표시하기 위한 Mapper다. */
    @Mock
    private SchoolMapper schoolMapper;

    /** 최종 로그에 공원 원천 개수를 표시하기 위한 Mapper다. */
    @Mock
    private ParkMapper parkMapper;

    /** 최종 로그에 상권 원천 업소 수를 표시하기 위한 Mapper다. */
    @Mock
    private CommercialMetricMapper commercialMetricMapper;

    private NeighborhoodRecommendServiceImpl recommendService;

    @BeforeEach
    void setUp() {
        recommendService = new NeighborhoodRecommendServiceImpl(
            neighborhoodRecommendMapper,
            List.of(cafeScoreCalculator, parkScoreCalculator, petScoreCalculator),
            schoolMapper,
            parkMapper,
            commercialMetricMapper
        );
    }

    @Test
    void 후보_동에_점수를_계산하고_지역명을_붙여_프론트_응답으로_반환한다() {
        // 1. 위치·예산 필터를 통과한 후보 법정동 3개가 Mapper에서 조회됐다고 가정한다.
        List<String> candidateDongCodes = List.of("1111010100", "1111010200", "1111010300");
        when(neighborhoodRecommendMapper.findLegalDongCodes(any()))
            .thenReturn(candidateDongCodes);

        // 2. 서비스 안으로 합쳐진 Calculator 점수 수집·가중치 합산을 검증하기 위해 카테고리별 점수를 고정한다.
        when(cafeScoreCalculator.category()).thenReturn(SurveyCategory.CAFE);
        when(parkScoreCalculator.category()).thenReturn(SurveyCategory.PARK);
        when(petScoreCalculator.category()).thenReturn(SurveyCategory.PET);
        when(cafeScoreCalculator.calculateScores(candidateDongCodes)).thenReturn(Map.of(
            "1111010100", 80.0, "1111010200", 90.0, "1111010300", 70.0));
        when(parkScoreCalculator.calculateScores(candidateDongCodes)).thenReturn(Map.of(
            "1111010100", 80.0, "1111010200", 80.0, "1111010300", 80.0));
        when(petScoreCalculator.calculateScores(candidateDongCodes)).thenReturn(Map.of(
            "1111010100", 80.0, "1111010200", 95.0, "1111010300", 65.0));

        // 3. 코드별 지역명은 dongcodes에서 별도로 읽어 점수 결과에 보강한다.
        when(neighborhoodRecommendMapper.findNeighborhoodsByDongCodes(anyList())).thenReturn(List.of(
            namedNeighborhood("1111010100", "서울특별시", "종로구", "청운동"),
            namedNeighborhood("1111010200", "서울특별시", "종로구", "신교동"),
            namedNeighborhood("1111010300", "서울특별시", "종로구", "궁정동")
        ));

        NeighborhoodRecommendResponse response = recommendService.recommend(request(
            List.of("CAFE", "PARK", "PET")
        ));

        // 4. 프론트가 바로 사용할 수 있게 코드·지역명·순위·점수가 함께 반환되는지 확인한다.
        assertThat(response.getTotalCount()).isEqualTo(3);
        assertThat(response.getPriorities()).containsExactly("CAFE", "PARK", "PET");
        assertThat(response.getRecommendations()).extracting(RecommendedNeighborhood::getLawdCd)
            .containsExactly("1111010200", "1111010100", "1111010300");
        assertThat(response.getRecommendations()).extracting(RecommendedNeighborhood::getDongName)
            .containsExactly("신교동", "청운동", "궁정동");
        assertThat(response.getRecommendations()).extracting(RecommendedNeighborhood::getScore)
            .containsExactly(88.0, 80.0, 72.0);

        // 새 요청의 매매가 최대값이 후보 SQL까지 그대로 전달되는지 확인한다.
        ArgumentCaptor<NeighborhoodRecommendRequest> requestCaptor = ArgumentCaptor.forClass(
            NeighborhoodRecommendRequest.class
        );
        verify(neighborhoodRecommendMapper).findLegalDongCodes(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDealAmountMax()).isEqualTo(30_000);
    }

    /** 프론트가 보내는 최소 추천 요청을 만든다. */
    private NeighborhoodRecommendRequest request(List<String> priorities) {
        NeighborhoodRecommendRequest request = new NeighborhoodRecommendRequest();
        request.setLatitude(37.5665);
        request.setLongitude(126.9780);
        request.setRadiusKm(10.0);
        request.setDealAmountMax(30000);
        request.setPriorities(priorities);
        return request;
    }

    /** 점수 계산 서비스가 반환한 순위·코드·점수만 있는 결과를 만든다. */
    private RecommendedNeighborhood scoredNeighborhood(int rank, String dongCode, double score) {
        RecommendedNeighborhood neighborhood = new RecommendedNeighborhood();
        neighborhood.setRank(rank);
        neighborhood.setLawdCd(dongCode);
        neighborhood.setScore(score);
        return neighborhood;
    }

    /** dongcodes 조회 결과처럼 지역명만 있는 객체를 만든다. */
    private RecommendedNeighborhood namedNeighborhood(
        String dongCode,
        String sidoName,
        String sigunguName,
        String dongName
    ) {
        RecommendedNeighborhood neighborhood = new RecommendedNeighborhood();
        neighborhood.setLawdCd(dongCode);
        neighborhood.setSidoName(sidoName);
        neighborhood.setSigunguName(sigunguName);
        neighborhood.setDongName(dongName);
        return neighborhood;
    }
}
