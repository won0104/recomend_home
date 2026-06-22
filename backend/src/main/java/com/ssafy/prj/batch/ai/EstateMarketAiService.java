package com.ssafy.prj.batch.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;

@Service
public class EstateMarketAiService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    @Value("${batch.ai.api-key:}")
    private String apiKey;

    @Value("${batch.ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${batch.ai.model:gpt-4o-mini}")
    private String model;

    public EstateMarketAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String summarize(EstateMarketReport report, List<EstateMarketItem> items) {
        String prompt = buildPrompt(report, items);
        if (isBlank(apiKey)) {
            return fallbackSummary(report, items);
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "너는 한국 부동산 데이터 분석 리포트를 작성하는 분석가다."),
                    Map.of("role", "user", "content", prompt)));

            String response = restClient.post()
                    .uri(normalizedBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.at("/choices/0/message/content").asText("");
            return isBlank(content) ? fallbackSummary(report, items) : content.trim();
        } catch (Exception e) {
            System.err.println("[EstateBatch-AI] AI call failed: " + e.getMessage());
            return fallbackSummary(report, items);
        }
    }

    private String buildPrompt(EstateMarketReport report, List<EstateMarketItem> items) {
        StringBuilder data = new StringBuilder();
        int limit = Math.min(items == null ? 0 : items.size(), 15);
        for (int i = 0; i < limit; i++) {
            EstateMarketItem item = items.get(i);
            data.append("- ")
                    .append(item.getDealDate()).append(" ")
                    .append(item.getAptName()).append(", ")
                    .append(item.getDong()).append(", ")
                    .append(item.getArea()).append("㎡, ")
                    .append(item.getFloor()).append("층, ")
                    .append(item.getDealAmount()).append("만원\n");
        }

        return """
                다음 아파트 실거래 데이터를 바탕으로 한국어 부동산 시장 요약 리포트를 작성해줘.
                불필요한 원문 반복은 제거하고, 거래량, 평균가, 최고/최저 거래, 유의할 점을 구조화해줘.
                결과는 제목, 핵심 요약 3개, 상세 분석, 운영 관점 메모 순서로 작성해줘.

                [배치 메타]
                리포트 ID: %s
                지역 코드: %s
                거래 월: %s
                거래 건수: %d
                평균 거래금액: %,d만원
                최고 거래금액: %,d만원
                최저 거래금액: %,d만원

                [거래 샘플]
                %s
                """.formatted(report.getReportId(), report.getLawdCd(), report.getDealYmd(),
                report.getItemCount(), report.getAvgDealAmount(), report.getMaxDealAmount(),
                report.getMinDealAmount(), data);
    }

    private String fallbackSummary(EstateMarketReport report, List<EstateMarketItem> items) {
        String topDeal = "-";
        if (items != null && !items.isEmpty()) {
            EstateMarketItem max = items.stream()
                    .max((a, b) -> Integer.compare(a.getDealAmount(), b.getDealAmount()))
                    .orElse(items.get(0));
            topDeal = max.getAptName() + " " + max.getDealAmount() + "만원";
        }

        return """
                %s

                핵심 요약
                - %s 지역의 %s 거래 데이터 %d건을 수집했습니다.
                - 평균 거래금액은 %,d만원이며, 최고 거래는 %s입니다.
                - 외부 AI API 키가 설정되지 않았거나 호출에 실패하여 로컬 규칙 기반 요약을 생성했습니다.

                상세 분석
                수집 데이터는 아파트명, 법정동, 전용면적, 층, 건축연도, 거래일, 거래금액만 남기도록 정제했습니다.
                이 요약은 배치 파이프라인의 실패 내성을 확인하기 위한 fallback 결과이며, 운영 환경에서는 batch.ai.api-key 설정 후 생성형 AI 요약을 사용할 수 있습니다.
                """.formatted(report.getTitle(), report.getLawdCd(), report.getDealYmd(),
                report.getItemCount(), report.getAvgDealAmount(), topDeal);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizedBaseUrl() {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.replaceAll("/+$", "");
    }
}
