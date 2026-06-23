package com.ssafy.prj.chat.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.prj.chat.dto.KakaoPlace;

@Service
public class KakaoApiServiceImpl implements KakaoApiService {

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    @Override
    public KakaoPlace searchFirstPlace(String keyword) {
        if (isBlank(keyword)) {
            throw new IllegalArgumentException("장소 키워드가 필요합니다.");
        }
        if (isBlank(kakaoRestApiKey)) {
            throw new IllegalStateException("kakao.rest-api-key 설정이 필요합니다.");
        }

        URI uri = UriComponentsBuilder
            .fromUriString("https://dapi.kakao.com/v2/local/search/keyword.json")
            .queryParam("query", keyword)
            .queryParam("size", 1)
            .build()
            .encode()
            .toUri();

        JsonNode response = restClient.get()
            .uri(uri)
            .header("Authorization", "KakaoAK " + kakaoRestApiKey)
            .retrieve()
            .body(JsonNode.class);

        JsonNode first = response == null ? null : response.path("documents").path(0);
        if (first == null || first.isMissingNode()) {
            throw new IllegalArgumentException("장소를 찾지 못했습니다: " + keyword);
        }

        return new KakaoPlace(
            first.path("place_name").asText(keyword),
            first.path("y").asDouble(),
            first.path("x").asDouble()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
