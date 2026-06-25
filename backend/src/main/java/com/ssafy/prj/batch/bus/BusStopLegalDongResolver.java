package com.ssafy.prj.batch.bus;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 새롭거나 좌표가 바뀐 버스 정류소의 WGS84 좌표를 법정동 코드로 바꾼다. */
@Component
public class BusStopLegalDongResolver {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${batch.bus.geocoding.api-url:https://dapi.kakao.com/v2/local/geo/coord2regioncode.json}")
    private String apiUrl;

    /** 브라우저 JavaScript 키가 아니라 서버 전용 REST API 키를 사용한다. */
    @Value("${batch.bus.geocoding.api-key:}")
    private String apiKey;

    /** 정류소가 새로 생긴 경우에도 외부 API 요청이 한꺼번에 몰리지 않도록 잠시 대기한다. */
    @Value("${batch.bus.geocoding.delay-ms:25}")
    private long delayMillis;

    public BusStopLegalDongResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 카카오 좌표→행정구역 API 응답 중 법정동(region_type=B) 코드만 반환한다.
     * 행정동(H) 코드는 dongcodes FK와 일치하지 않으므로 사용하지 않는다.
     */
    public String resolveLegalDongCode(BigDecimal latitude, BigDecimal longitude) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("batch.bus.geocoding.api-key is required");
        }

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("x", longitude.toPlainString())
            .queryParam("y", latitude.toPlainString())
            .queryParam("input_coord", "WGS84")
            .queryParam("output_coord", "WGS84")
            .build()
            .encode()
            .toUri();

        String response = restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
            .retrieve()
            .body(String.class);

        JsonNode documents = objectMapper.readTree(response).path("documents");
        if (!documents.isArray()) {
            return "";
        }

        for (JsonNode document : documents) {
            if ("B".equals(document.path("region_type").asText())) {
                String dongCode = document.path("code").asText().trim();
                if (!dongCode.isBlank()) {
                    sleepBetweenRequests();
                    return dongCode;
                }
            }
        }

        sleepBetweenRequests();
        return "";
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
}
