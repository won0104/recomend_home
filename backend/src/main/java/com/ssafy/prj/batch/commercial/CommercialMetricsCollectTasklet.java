package com.ssafy.prj.batch.commercial;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommercialMetricsCollectTasklet implements Tasklet {

    static final String CAFE_RESTAURANT = "CAFE_RESTAURANT";

    private final CommercialMetricsBatchDao commercialMetricsBatchDao;
    private final RestClient restClient = RestClient.create();

    @Value("${batch.commercial.api-url}")
    private String apiUrl;

    @Value("${batch.commercial.api-key:}")
    private String apiKey;

    @Value("${batch.commercial.region-code:11}")
    private String regionCode;

    @Value("${batch.commercial.industry-code:I2}")
    private String industryCode;

    @Value("${batch.commercial.page-size:1000}")
    private int pageSize;

    @Override
    @Transactional
    public org.springframework.batch.repeat.RepeatStatus execute(
        StepContribution contribution,
        ChunkContext chunkContext
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("batch.commercial.api-key is required");
        }

        CollectedMetrics collected = collectAllPages();
        List<DongCommercialMetric> metrics = toMetricsForExistingDongCodes(collected);

        commercialMetricsBatchDao.deleteByCategory(CAFE_RESTAURANT);
        if (!metrics.isEmpty()) {
            commercialMetricsBatchDao.insertMetrics(metrics);
        }

        System.out.println("[CommercialMetricsBatch] category=" + CAFE_RESTAURANT
            + ", sourceDate=" + collected.sourceDate()
            + ", totalStores=" + collected.totalStores()
            + ", storedDongs=" + metrics.size());
        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
    }

    private CollectedMetrics collectAllPages() throws Exception {
        Map<String, Integer> countsByDongCode = new HashMap<>();
        Set<String> businessIds = new HashSet<>();
        int totalCount = -1;
        LocalDate sourceDate = null;

        for (int pageNo = 1; totalCount < 0 || (pageNo - 1) * pageSize < totalCount; pageNo++) {
            Document response = requestPage(pageNo);
            validateResponse(response);

            if (totalCount < 0) {
                totalCount = Integer.parseInt(firstText(response, "totalCount"));
                sourceDate = parseSourceDate(firstText(response, "stdrYm"));
            }

            NodeList items = response.getElementsByTagName("item");
            if (items.getLength() == 0) {
                break;
            }

            int itemCount = 0;
            for (int index = 0; index < items.getLength(); index++) {
                itemCount++;
                Element item = (Element) items.item(index);
                String businessId = firstText(item, "bizesId");
                String dongCode = firstText(item, "ldongCd");
                if (businessId.isBlank() || dongCode.length() != 10 || !businessIds.add(businessId)) {
                    continue;
                }
                countsByDongCode.merge(dongCode, 1, Integer::sum);
            }
            if (itemCount == 0) {
                break;
            }
            if (pageNo == 1 || pageNo % 10 == 0 || pageNo * pageSize >= totalCount) {
                System.out.println("[CommercialMetricsBatch] page=" + pageNo
                    + ", totalPages=" + (int) Math.ceil((double) totalCount / pageSize)
                    + ", uniqueStores=" + businessIds.size());
            }
        }

        if (totalCount < 0 || sourceDate == null) {
            throw new IllegalStateException("Commercial API returned no readable page");
        }
        return new CollectedMetrics(countsByDongCode, businessIds.size(), sourceDate);
    }

    private Document requestPage(int pageNo) throws Exception {
        String encodedServiceKey = URLEncoder.encode(
            normalizeServiceKey(apiKey), StandardCharsets.UTF_8
        );
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("serviceKey", encodedServiceKey)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", pageSize)
            .queryParam("divId", "ctprvnCd")
            .queryParam("key", regionCode)
            .queryParam("indsLclsCd", industryCode)
            .queryParam("type", "xml")
            .build(true)
            .toUri();

        String body = restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .body(String.class);
        return parseXml(body);
    }

    /**
     * Data Portal shows both an encoded and a decoded service key. Decode only
     * percent-encoded input here, then let UriComponentsBuilder encode once.
     */
    private String normalizeServiceKey(String configuredKey) {
        return configuredKey.contains("%")
            ? URLDecoder.decode(configuredKey, StandardCharsets.UTF_8)
            : configuredKey;
    }

    private void validateResponse(Document response) {
        if (!"00".equals(firstText(response, "resultCode"))) {
            throw new IllegalStateException("Commercial API failed: "
                + firstText(response, "resultMsg"));
        }
    }

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new java.io.StringReader(xml)));
    }

    private String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private List<DongCommercialMetric> toMetricsForExistingDongCodes(CollectedMetrics collected) {
        if (collected.countsByDongCode().isEmpty()) {
            return List.of();
        }
        Set<String> existingCodes = new HashSet<>(
            commercialMetricsBatchDao.findExistingDongCodes(new ArrayList<>(collected.countsByDongCode().keySet()))
        );
        return collected.countsByDongCode().entrySet().stream()
            .filter(entry -> existingCodes.contains(entry.getKey()))
            .map(entry -> new DongCommercialMetric(
                entry.getKey(), CAFE_RESTAURANT, entry.getValue(), collected.sourceDate()))
            .toList();
    }

    private LocalDate parseSourceDate(String standardYearMonth) {
        if (!standardYearMonth.matches("\\d{6}")) {
            throw new IllegalStateException("Invalid commercial API stdrYm: " + standardYearMonth);
        }
        return YearMonth.parse(standardYearMonth, java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))
            .atDay(1);
    }

    private record CollectedMetrics(
        Map<String, Integer> countsByDongCode,
        int totalStores,
        LocalDate sourceDate
    ) {}
}
