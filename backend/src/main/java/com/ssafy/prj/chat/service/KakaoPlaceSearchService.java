package com.ssafy.prj.chat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.prj.chat.domain.ChatPlaceCandidate;

@Service
public class KakaoPlaceSearchService {
    private final RestClient restClient = RestClient.create();

    @Value("${kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    public List<ChatPlaceCandidate> search(String keyword) {
        if (keyword == null || keyword.isBlank() || kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("dapi.kakao.com")
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", keyword.trim())
                            .queryParam("size", 5)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .body(JsonNode.class);

            List<ChatPlaceCandidate> candidates = new ArrayList<>();
            if (root == null || !root.has("documents")) {
                return candidates;
            }
            for (JsonNode node : root.get("documents")) {
                Double longitude = parseDouble(node.path("x").asText(null));
                Double latitude = parseDouble(node.path("y").asText(null));
                if (latitude == null || longitude == null) {
                    continue;
                }
                String address = node.path("road_address_name").asText("");
                if (address.isBlank()) {
                    address = node.path("address_name").asText("");
                }
                candidates.add(new ChatPlaceCandidate(
                        node.path("id").asText(""),
                        node.path("place_name").asText(""),
                        address,
                        latitude,
                        longitude));
            }
            return candidates;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
