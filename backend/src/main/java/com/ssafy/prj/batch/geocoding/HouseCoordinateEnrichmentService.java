package com.ssafy.prj.batch.geocoding;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.batch.dao.EstateMarketBatchDao;
import com.ssafy.prj.batch.domain.HouseCoordinateTarget;

/**
 * 매매 배치로 새로 들어온 아파트의 좌표를 카카오 로컬 API로 한 번만 보강한다.
 *
 * <p>동네 추천의 반경 계산과 매물 지도 마커는 {@code houseinfos.latitude/longitude}를 사용한다.
 * 국토부 매매 API는 좌표를 주지 않으므로, DB에 좌표가 없는 아파트만 주소 검색한다. 좌표를 저장한
 * 아파트는 다음 배치에서 조회 대상에서 제외되므로, 거래 건수가 늘어도 같은 주소를 반복 호출하지 않는다.</p>
 */
@Component
public class HouseCoordinateEnrichmentService {

    private final EstateMarketBatchDao estateMarketBatchDao;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${batch.estate.geocoding.enabled:true}")
    private boolean enabled;

    @Value("${batch.estate.geocoding.api-url:https://dapi.kakao.com/v2/local/search/address.json}")
    private String apiUrl;

    /** application-secret.properties의 KAKAO_REST_API_KEY에서 주입한다. */
    @Value("${batch.estate.geocoding.api-key:}")
    private String apiKey;

    /** 호출 간 짧은 간격을 둬 외부 API에 한꺼번에 요청이 몰리지 않게 한다. */
    @Value("${batch.estate.geocoding.delay-ms:25}")
    private long delayMillis;

    public HouseCoordinateEnrichmentService(
        EstateMarketBatchDao estateMarketBatchDao,
        ObjectMapper objectMapper
    ) {
        this.estateMarketBatchDao = estateMarketBatchDao;
        this.objectMapper = objectMapper;
    }

    /**
     * 이번 reportId에서 새로 적재된 아파트 중 아직 좌표가 없는 대상만 처리한다.
     * API 키가 설정되지 않았어도 매매 거래 적재 자체는 성공해야 하므로, 좌표 보강만 건너뛴다.
     */
    public void enrichForReport(String reportId) {
        if (!enabled) {
            System.out.println("[EstateBatch-Geocoding] 좌표 보강 기능이 비활성화되어 건너뜁니다.");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[EstateBatch-Geocoding] KAKAO_REST_API_KEY가 없어 좌표 보강을 건너뜁니다.");
            return;
        }

        var targets = estateMarketBatchDao.findMissingCoordinateTargetsByReportId(reportId);
        int successCount = 0;
        int failedCount = 0;
        for (HouseCoordinateTarget target : targets) {
            try {
                Coordinate coordinate = requestCoordinate(target.address());
                if (coordinate == null) {
                    failedCount++;
                    System.out.println("[EstateBatch-Geocoding] 주소 검색 결과 없음. aptSeq="
                        + target.aptSeq() + ", address=" + target.address());
                } else {
                    estateMarketBatchDao.updateHouseCoordinates(
                        target.aptSeq(), coordinate.latitude(), coordinate.longitude());
                    successCount++;
                }
            } catch (Exception exception) {
                // 특정 주소 하나의 실패가 전체 매매 적재를 롤백시키지 않도록 해당 대상만 기록하고 계속 진행한다.
                failedCount++;
                System.err.println("[EstateBatch-Geocoding] 좌표 보강 실패. aptSeq="
                    + target.aptSeq() + ", reason=" + exception.getMessage());
            }
            sleepBetweenRequests();
        }
        System.out.println("[EstateBatch-Geocoding] reportId=" + reportId
            + ", targets=" + targets.size()
            + ", success=" + successCount
            + ", failed=" + failedCount);
    }

    /** 카카오 주소→좌표 API 응답의 x(경도), y(위도)만 읽는다. */
    private Coordinate requestCoordinate(String address) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("query", address)
            .build()
            .encode()
            .toUri();

        String response = restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
            .retrieve()
            .body(String.class);

        JsonNode documents = objectMapper.readTree(response).path("documents");
        if (!documents.isArray() || documents.isEmpty()) {
            return null;
        }
        JsonNode first = documents.get(0);
        String longitude = first.path("x").asText();
        String latitude = first.path("y").asText();
        if (longitude.isBlank() || latitude.isBlank()) {
            return null;
        }
        return new Coordinate(latitude, longitude);
    }

    private void sleepBetweenRequests() {
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record Coordinate(String latitude, String longitude) {
    }
}
