package com.ssafy.prj.batch.tasklet;

import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ssafy.prj.batch.dao.EstateMarketBatchDao;
import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;
import com.ssafy.prj.batch.shared.BatchContextKeys;

@Component
public class EstateMarketCollectTasklet implements Tasklet {

    /** 대량 INSERT 문을 적절한 크기로 나눠 MySQL 패킷과 SQL 길이 부담을 낮춘다. */
    private static final int ITEM_INSERT_CHUNK_SIZE = 500;

    private final RestClient restClient = RestClient.create();
    private final EstateMarketBatchDao estateMarketBatchDao;

    @Value("${batch.estate.api-url}")
    private String apiUrl;

    @Value("${batch.estate.api-key:}")
    private String apiKey;

    @Value("${batch.estate.lawd-cd:11110}")
    private String lawdCd;

    @Value("${batch.estate.lawd-codes:}")
    private String lawdCodes;

    /** 비어 있지 않으면 dongcodes에서 해당 시·도의 시군구 코드만 조회한다. */
    @Value("${batch.estate.sido-name:}")
    private String sidoName;

    @Value("${batch.estate.deal-ymd:}")
    private String configuredDealYmd;

    @Value("${batch.estate.months:1}")
    private int months;

    @Value("${batch.estate.page-size:100}")
    private int pageSize;

    @Value("${batch.estate.max-pages:0}")
    private int maxPages;

    @Value("${batch.estate.max-items:0}")
    private int maxItems;

    @Value("${batch.estate.force-api:false}")
    private boolean forceApi;

    public EstateMarketCollectTasklet(EstateMarketBatchDao estateMarketBatchDao) {
        this.estateMarketBatchDao = estateMarketBatchDao;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 외부 API가 일시 실패했을 때 로컬 거래 데이터로 대체 조회해도 테이블 누락으로 실패하지 않게 한다.
        estateMarketBatchDao.createHouseInfoTable();
        estateMarketBatchDao.createHouseDealsTable();

        List<String> targetLawdCodes = resolveLawdCodes();
        List<String> targetDealYmds = resolveDealYmds();
        String reportId = "ESTATE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        List<EstateMarketItem> items = new ArrayList<>();
        if (forceApi) {
            items.addAll(collectAllFromOpenApi(targetLawdCodes, targetDealYmds));
        }

        if (items.isEmpty()) {
            items.addAll(collectFallback(targetLawdCodes, targetDealYmds));
        }

        for (EstateMarketItem item : items) {
            item.setReportId(reportId);
        }

        EstateMarketReport report = buildReport(reportId, targetLawdCodes, targetDealYmds, items);

        /*
         * 이전 구현은 items 전체를 ExecutionContext에 넣어 다음 Step으로 전달했다.
         * 2,067건처럼 데이터가 커지면 Spring Batch 메타 테이블의 SERIALIZED_CONTEXT가 넘쳐
         * Job 자체가 실패한다. 따라서 원본 거래 목록은 배치 전용 테이블에 저장하고,
         * ExecutionContext에는 작고 안전한 reportId만 남긴다.
         */
        estateMarketBatchDao.createReportTable();
        estateMarketBatchDao.widenReportDealYmdColumn();
        estateMarketBatchDao.createItemTable();
        estateMarketBatchDao.insertReport(report);
        for (int start = 0; start < items.size(); start += ITEM_INSERT_CHUNK_SIZE) {
            List<EstateMarketItem> chunk = items.subList(start,
                    Math.min(start + ITEM_INSERT_CHUNK_SIZE, items.size()));
            estateMarketBatchDao.insertItems(chunk);
        }

        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        ctx.put(BatchContextKeys.REPORT_ID, reportId);

        System.out.println("[EstateBatch-Collect] reportId=" + reportId
                + ", lawdCodes=" + targetLawdCodes.size()
                + ", months=" + targetDealYmds
                + ", items=" + items.size());
        return RepeatStatus.FINISHED;
    }

    private List<EstateMarketItem> collectAllFromOpenApi(List<String> targetLawdCodes, List<String> targetDealYmds) {
        if (isBlank(apiKey)) {
            System.out.println("[EstateBatch-Collect] batch.estate.api-key is empty. Use local DB fallback.");
            return List.of();
        }

        List<EstateMarketItem> items = new ArrayList<>();
        for (String targetDealYmd : targetDealYmds) {
            for (String targetLawdCd : targetLawdCodes) {
                items.addAll(collectPagesFromOpenApi(targetLawdCd, targetDealYmd));
                if (reachedMaxItems(items)) {
                    return items;
                }
            }
        }
        return items;
    }

    private List<EstateMarketItem> collectPagesFromOpenApi(String targetLawdCd, String targetDealYmd) {
        List<EstateMarketItem> items = new ArrayList<>();
        int pageNo = 1;
        while (maxPages <= 0 || pageNo <= maxPages) {
            List<EstateMarketItem> pageItems = collectOpenApiPage(targetLawdCd, targetDealYmd, pageNo);
            if (pageItems.isEmpty()) {
                break;
            }
            items.addAll(pageItems);
            if (pageItems.size() < pageSize || reachedMaxItems(items)) {
                break;
            }
            pageNo++;
        }
        return items;
    }

    private List<EstateMarketItem> collectOpenApiPage(String targetLawdCd, String targetDealYmd, int pageNo) {
        try {
            // 서비스키에 포함될 수 있는 +, /, = 문자를 쿼리 파라미터에 안전하게 넣는다.
            // 설정 파일에는 원본 키와 URL 인코딩된 키 모두 허용한다.
            String encodedServiceKey = URLEncoder.encode(normalizeServiceKey(apiKey), StandardCharsets.UTF_8);
            URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("serviceKey", encodedServiceKey)
                    .queryParam("LAWD_CD", targetLawdCd)
                    .queryParam("DEAL_YMD", targetDealYmd)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", pageSize)
                    .build(true)
                    .toUri();

            String xml = restClient.get().uri(uri).accept(MediaType.APPLICATION_XML).retrieve().body(String.class);
            if (isBlank(xml)) {
                return List.of();
            }
            List<EstateMarketItem> items = parseItems(xml, targetLawdCd, targetDealYmd);
            if (!items.isEmpty()) {
                System.out.println("[EstateBatch-Collect] OPEN_API lawdCd=" + targetLawdCd
                        + ", dealYmd=" + targetDealYmd + ", page=" + pageNo + ", items=" + items.size());
            }
            return items;
        } catch (Exception e) {
            System.err.println("[EstateBatch-Collect] external API failed. lawdCd=" + targetLawdCd
                    + ", dealYmd=" + targetDealYmd + ", page=" + pageNo + ": " + e.getMessage());
            return List.of();
        }
    }

    private String normalizeServiceKey(String configuredKey) {
        return configuredKey.contains("%")
                ? URLDecoder.decode(configuredKey, StandardCharsets.UTF_8)
                : configuredKey;
    }

    private List<EstateMarketItem> collectFallback(List<String> targetLawdCodes, List<String> targetDealYmds) {
        List<EstateMarketItem> items = new ArrayList<>();
        int fallbackLimit = maxItems > 0 ? maxItems : pageSize;
        for (String targetDealYmd : targetDealYmds) {
            for (String targetLawdCd : targetLawdCodes) {
                List<EstateMarketItem> fallbackItems =
                        estateMarketBatchDao.selectFallbackItems(targetLawdCd, targetDealYmd, fallbackLimit);
                for (EstateMarketItem item : fallbackItems) {
                    item.setSourceType("LOCAL_DB");
                    item.setLawdCd(emptyToDefault(item.getLawdCd(), targetLawdCd));
                    item.setDealYmd(emptyToDefault(item.getDealYmd(), targetDealYmd));
                }
                items.addAll(fallbackItems);
                if (reachedMaxItems(items)) {
                    return items;
                }
            }
        }
        return items;
    }

    private List<EstateMarketItem> parseItems(String xml, String targetLawdCd, String targetDealYmd) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        var doc = builder.parse(new InputSource(new StringReader(xml)));
        NodeList nodes = doc.getElementsByTagName("item");

        List<EstateMarketItem> items = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element itemNode = (Element) nodes.item(i);
            EstateMarketItem item = new EstateMarketItem();
            item.setSourceType("OPEN_API");
            item.setLawdCd(targetLawdCd);
            item.setDealYmd(targetDealYmd);
            item.setAptName(firstText(itemNode, "aptNm", "aptName", "아파트"));
            item.setDong(firstText(itemNode, "umdNm", "법정동"));
            item.setJibun(firstText(itemNode, "jibun", "지번"));
            item.setDealAmount(parseInt(firstText(itemNode, "dealAmount", "거래금액")));
            item.setArea(parseDouble(firstText(itemNode, "excluUseAr", "전용면적")));
            item.setFloor(parseInt(firstText(itemNode, "floor", "층")));
            item.setBuildYear(parseInt(firstText(itemNode, "buildYear", "건축년도")));
            item.setDealDate(toDealDate(firstText(itemNode, "dealYear", "년"),
                    firstText(itemNode, "dealMonth", "월"),
                    firstText(itemNode, "dealDay", "일")));
            item.setRawAddress((item.getDong() + " " + item.getJibun()).trim());
            if (!isBlank(item.getAptName())) {
                items.add(item);
            }
        }
        return items;
    }

    private EstateMarketReport buildReport(String reportId, List<String> targetLawdCodes,
            List<String> targetDealYmds, List<EstateMarketItem> items) {
        EstateMarketReport report = new EstateMarketReport();
        report.setReportId(reportId);
        report.setTitle("부동산 실거래 배치 리포트 " + String.join(",", targetDealYmds));
        report.setLawdCd(targetLawdCodes.size() == 1 ? targetLawdCodes.get(0) : "ALL");
        report.setDealYmd(String.join(",", targetDealYmds));
        report.setCollectedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.setItemCount(items.size());
        report.setAiStatus("PENDING");

        if (!items.isEmpty()) {
            int sum = items.stream().mapToInt(EstateMarketItem::getDealAmount).sum();
            report.setAvgDealAmount(Math.round((float) sum / items.size()));
            report.setMaxDealAmount(items.stream().max(Comparator.comparingInt(EstateMarketItem::getDealAmount))
                    .map(EstateMarketItem::getDealAmount).orElse(0));
            report.setMinDealAmount(items.stream().min(Comparator.comparingInt(EstateMarketItem::getDealAmount))
                    .map(EstateMarketItem::getDealAmount).orElse(0));
        }
        return report;
    }

    private List<String> resolveLawdCodes() {
        if (!isBlank(sidoName)) {
            return estateMarketBatchDao.selectLawdCodesBySido(sidoName.trim());
        }
        if ("ALL".equalsIgnoreCase(lawdCodes.trim())) {
            return estateMarketBatchDao.selectAllLawdCodes();
        }
        if (!isBlank(lawdCodes)) {
            return splitCsv(lawdCodes);
        }
        return splitCsv(lawdCd);
    }

    private List<String> resolveDealYmds() {
        if (!isBlank(configuredDealYmd)) {
            return splitCsv(configuredDealYmd);
        }
        int count = Math.max(1, months);
        YearMonth start = YearMonth.now().minusMonths(1);
        List<String> dealYmds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dealYmds.add(start.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM")));
        }
        return dealYmds;
    }

    private List<String> splitCsv(String value) {
        Set<String> values = new LinkedHashSet<>();
        if (value != null) {
            for (String part : value.split(",")) {
                if (!isBlank(part)) {
                    values.add(part.trim());
                }
            }
        }
        return new ArrayList<>(values);
    }

    private boolean reachedMaxItems(List<EstateMarketItem> items) {
        return maxItems > 0 && items.size() >= maxItems;
    }

    private String firstText(Element element, String... names) {
        for (String name : names) {
            NodeList nodes = element.getElementsByTagName(name);
            if (nodes.getLength() > 0 && nodes.item(0) != null) {
                String value = nodes.item(0).getTextContent();
                if (!isBlank(value)) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private String toDealDate(String year, String month, String day) {
        if (isBlank(year) || isBlank(month) || isBlank(day)) {
            return "";
        }
        return "%s-%02d-%02d".formatted(year.trim(), parseInt(month), parseInt(day));
    }

    private int parseInt(String value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String emptyToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
