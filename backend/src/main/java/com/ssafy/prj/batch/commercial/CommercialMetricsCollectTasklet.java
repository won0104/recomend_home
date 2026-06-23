package com.ssafy.prj.batch.commercial;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;

/**
 * 서울시 상권 API를 읽어 추천용 "동별 업소 수"만 저장하는 Spring Batch Tasklet이다.
 *
 * <h2>전체 실행 흐름</h2>
 * <ol>
 *   <li>{@link CommercialMetricCategory}에 정의한 대분류·중분류 조합을 차례로 처리한다.</li>
 *   <li>카테고리별로 API의 모든 페이지를 요청한다.</li>
 *   <li>응답의 법정동 코드({@code ldongCd})별로 중복 없는 업소 수를 누적한다.</li>
 *   <li>우리 {@code dongcodes}에 존재하는 법정동만 남긴다.</li>
 *   <li>해당 카테고리의 이전 통계를 삭제하고, 새 집계 결과를 저장한다.</li>
 * </ol>
 *
 * <p>원본 업소 목록 전체를 DB에 저장하지 않는 이유는 추천에 필요한 값이 "동별 업소 수"뿐이기
 * 때문이다. 이 방식이면 추천 API가 호출될 때마다 외부 API를 호출하지 않아도 된다.</p>
 *
 * <p>이 Tasklet이 실행되는 Step 전체는 하나의 트랜잭션이다. 일곱 카테고리 중 하나라도 수집 또는
 * 저장에 실패하면 예외가 발생하고, 이번 실행에서 삭제·삽입한 모든 통계는 롤백된다. 따라서
 * 실패한 배치가 기존의 정상 통계를 빈 값으로 덮어쓰지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class CommercialMetricsCollectTasklet implements Tasklet {

    private final CommercialMetricsBatchDao commercialMetricsBatchDao;
    private final RestClient restClient = RestClient.create();

    /** 공공데이터포털 상권 API의 기본 주소다. */
    @Value("${batch.commercial.api-url}")
    private String apiUrl;

    /** application-secret.properties 또는 환경변수에서 읽는 서비스 키다. */
    @Value("${batch.commercial.api-key:}")
    private String apiKey;

    /** 서울특별시 코드. API의 {@code divId=ctprvnCd}와 함께 사용한다. */
    @Value("${batch.commercial.region-code:11}")
    private String regionCode;

    /** API 한 번에 받을 최대 행 수다. 페이지 수를 줄이기 위해 기본 1,000건을 사용한다. */
    @Value("${batch.commercial.page-size:1000}")
    private int pageSize;

    /**
     * Spring Batch가 Step을 시작할 때 한 번 호출한다.
     * 이 메서드는 개별 카테고리의 대·중분류 코드를 알 필요 없이, enum에 정의된 항목을 순서대로 실행한다.
     */
    @Override
    public org.springframework.batch.repeat.RepeatStatus execute(
        StepContribution contribution,
        ChunkContext chunkContext
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("batch.commercial.api-key is required");
        }

        for (CommercialMetricCategory category : CommercialMetricCategory.values()) {
            collectAndReplaceCategoryMetrics(category);
        }
        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
    }

    /**
     * 한 추천 카테고리의 최신 스냅샷을 만든다.
     *
     * <p>중요한 순서는 "성공적으로 전 페이지를 수집한 뒤 삭제"다. 외부 API 요청 도중 실패한 경우
     * 이 메서드가 삭제까지 도달하지 못하며, Step 트랜잭션도 롤백되므로 기존 통계가 보존된다.</p>
     */
    private void collectAndReplaceCategoryMetrics(CommercialMetricCategory category) throws Exception {
        CollectedMetrics collected = collectAllPages(category);
        List<DongCommercialMetric> metrics = toMetricsForExistingDongCodes(collected, category);

        commercialMetricsBatchDao.deleteByCategory(category.getDatabaseCategory());
        if (!metrics.isEmpty()) {
            commercialMetricsBatchDao.insertMetrics(metrics);
        }

        System.out.println("[CommercialMetricsBatch] category=" + category.getDatabaseCategory()
            + ", industryCode=" + category.getIndustryCode()
            + ", mediumIndustryCode=" + category.getMediumIndustryCode()
            + ", sourceDate=" + collected.sourceDate()
            + ", totalStores=" + collected.totalStores()
            + ", storedDongs=" + metrics.size());
    }

    /**
     * 지정한 업종 코드의 모든 페이지를 읽고 {@code 법정동 코드 -> 업소 수} 형태로 압축한다.
     *
     * <p>API는 페이지 경계에서 같은 업소가 중복될 가능성을 완전히 배제할 수 없으므로,
     * {@code bizesId}를 Set에 넣어 같은 업소를 한 번만 센다.</p>
     */
    private CollectedMetrics collectAllPages(CommercialMetricCategory category) throws Exception {
        Map<String, Integer> countsByDongCode = new HashMap<>();
        Set<String> businessIds = new HashSet<>();
        int totalCount = -1;
        LocalDate sourceDate = null;

        for (int pageNo = 1; totalCount < 0 || (pageNo - 1) * pageSize < totalCount; pageNo++) {
            Document response = requestPage(category, pageNo);
            validateResponse(response, category);

            // 첫 페이지에서만 전체 건수와 기준 연월을 읽으면 다음 페이지 수를 계산할 수 있다.
            if (totalCount < 0) {
                totalCount = Integer.parseInt(firstText(response, "totalCount"));
                sourceDate = parseSourceDate(firstText(response, "stdrYm"));
            }

            NodeList items = response.getElementsByTagName("item");
            if (items.getLength() == 0) {
                break;
            }

            for (int index = 0; index < items.getLength(); index++) {
                Element item = (Element) items.item(index);
                String businessId = firstText(item, "bizesId");
                String dongCode = firstText(item, "ldongCd");

                // 법정동 코드가 없거나 형식이 맞지 않는 업소, 이미 센 업소는 추천 통계에서 제외한다.
                if (businessId.isBlank() || dongCode.length() != 10 || !businessIds.add(businessId)) {
                    continue;
                }
                countsByDongCode.merge(dongCode, 1, Integer::sum);
            }

            // 긴 수집 작업의 진행 상태를 서버 로그에서 확인할 수 있게 한다.
            if (pageNo == 1 || pageNo % 10 == 0 || pageNo * pageSize >= totalCount) {
                System.out.println("[CommercialMetricsBatch] category=" + category.getDatabaseCategory()
                    + ", page=" + pageNo
                    + ", totalPages=" + (int) Math.ceil((double) totalCount / pageSize)
                    + ", uniqueStores=" + businessIds.size());
            }
        }

        if (totalCount < 0 || sourceDate == null) {
            throw new IllegalStateException("Commercial API returned no readable page: "
                + category.getIndustryCode());
        }
        return new CollectedMetrics(countsByDongCode, businessIds.size(), sourceDate);
    }

    /**
     * API 요청 URI를 만들고 XML 응답을 DOM 문서로 변환한다.
     *
     * <p>대분류는 모든 카테고리에 필수다. 중분류가 정의된 카테고리만 {@code indsMclsCd}를 추가한다.
     * 예를 들어 식료품 소매는 {@code indsLclsCd=G2&indsMclsCd=G205}로 요청한다.</p>
     */
    private Document requestPage(CommercialMetricCategory category, int pageNo) throws Exception {
        // 설정 파일에는 인코딩된 키와 원본 키 어느 쪽이든 들어갈 수 있다.
        // 한 번 원본 형태로 정규화한 후 URI에 넣어 이중 인코딩을 방지한다.
        String encodedServiceKey = URLEncoder.encode(normalizeServiceKey(apiKey), StandardCharsets.UTF_8);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("serviceKey", encodedServiceKey)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", pageSize)
            .queryParam("divId", "ctprvnCd")
            .queryParam("key", regionCode)
            .queryParam("indsLclsCd", category.getIndustryCode());

        // null인 중분류를 빈 파라미터로 보내면 API가 다른 결과를 반환할 수 있으므로 아예 추가하지 않는다.
        if (category.getMediumIndustryCode() != null) {
            uriBuilder.queryParam("indsMclsCd", category.getMediumIndustryCode());
        }

        URI uri = uriBuilder
            // 현재 API는 JSON보다 XML 응답에서 정상 인증되는 것을 확인해 XML로 고정한다.
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
     * Data Portal 화면에서는 서비스 키를 URL 인코딩한 형태와 원본 형태 모두 보여준다.
     * '%'가 있을 때만 한 번 디코딩해, URI 생성 과정에서 키가 두 번 인코딩되지 않게 한다.
     */
    private String normalizeServiceKey(String configuredKey) {
        return configuredKey.contains("%")
            ? URLDecoder.decode(configuredKey, StandardCharsets.UTF_8)
            : configuredKey;
    }

    /** API 헤더의 resultCode가 성공(00)이 아니면 DB를 변경하지 않고 배치를 실패 처리한다. */
    private void validateResponse(Document response, CommercialMetricCategory category) {
        if (!"00".equals(firstText(response, "resultCode"))) {
            throw new IllegalStateException("Commercial API failed for " + category.getIndustryCode() + ": "
                + firstText(response, "resultMsg"));
        }
    }

    /**
     * 외부 XML을 안전하게 파싱한다. DOCTYPE을 금지해 외부 엔터티(XXE)를 읽지 못하게 한다.
     */
    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new java.io.StringReader(xml)));
    }

    /** 문서 전체에서 첫 번째 태그의 텍스트를 읽는다. 없는 태그는 빈 문자열로 처리한다. */
    private String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    /** 한 업소 item 내부에서 태그의 텍스트를 읽는다. 없는 태그는 빈 문자열로 처리한다. */
    private String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    /**
     * API의 법정동 코드 중 우리 서비스의 {@code dongcodes}에 실제로 등록된 코드만 남긴다.
     * 외부 데이터의 코드가 FK 제약을 위반해 배치 전체가 실패하는 것을 막는다.
     */
    private List<DongCommercialMetric> toMetricsForExistingDongCodes(
        CollectedMetrics collected,
        CommercialMetricCategory category
    ) {
        if (collected.countsByDongCode().isEmpty()) {
            return List.of();
        }

        Set<String> existingCodes = new HashSet<>(commercialMetricsBatchDao.findExistingDongCodes(
            new ArrayList<>(collected.countsByDongCode().keySet())
        ));

        return collected.countsByDongCode().entrySet().stream()
            .filter(entry -> existingCodes.contains(entry.getKey()))
            .map(entry -> new DongCommercialMetric(
                entry.getKey(),
                category.getDatabaseCategory(),
                entry.getValue(),
                collected.sourceDate()
            ))
            .toList();
    }

    /** API 기준 연월(예: 202606)을 해당 월의 첫 날 Date 값으로 저장한다. */
    private LocalDate parseSourceDate(String standardYearMonth) {
        if (!standardYearMonth.matches("\\d{6}")) {
            throw new IllegalStateException("Invalid commercial API stdrYm: " + standardYearMonth);
        }
        return YearMonth.parse(standardYearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
    }

    /** 한 카테고리의 API 응답 전체를 순회한 뒤 얻는, 아직 DB 저장 전 집계 결과다. */
    private record CollectedMetrics(
        Map<String, Integer> countsByDongCode,
        int totalStores,
        LocalDate sourceDate
    ) {}
}
