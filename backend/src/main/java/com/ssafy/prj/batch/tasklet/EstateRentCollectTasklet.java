package com.ssafy.prj.batch.tasklet;

import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

import com.ssafy.prj.batch.dao.EstateRentBatchDao;
import com.ssafy.prj.batch.domain.EstateRentItem;
import com.ssafy.prj.batch.domain.EstateRentReport;
import com.ssafy.prj.batch.shared.BatchContextKeys;

/** 국토교통부 아파트 전월세 API를 호출하고, 다음 저장 단계가 읽을 임시 테이블에 적재한다. */
@Component
public class EstateRentCollectTasklet implements Tasklet {

    /** 한 번의 INSERT SQL에 너무 많은 행이 들어가지 않도록 나눈다. */
    private static final int ITEM_INSERT_CHUNK_SIZE = 500;

    private final RestClient restClient = RestClient.create();
    private final EstateRentBatchDao estateRentBatchDao;

    @Value("${batch.estate-rent.api-url}")
    private String apiUrl;

    @Value("${batch.estate-rent.api-key:}")
    private String apiKey;

    @Value("${batch.estate-rent.lawd-cd:11110}")
    private String lawdCd;

    @Value("${batch.estate-rent.lawd-codes:}")
    private String lawdCodes;

    @Value("${batch.estate-rent.sido-name:}")
    private String sidoName;

    @Value("${batch.estate-rent.deal-ymd:}")
    private String configuredDealYmd;

    @Value("${batch.estate-rent.months:1}")
    private int months;

    @Value("${batch.estate-rent.page-size:1000}")
    private int pageSize;

    @Value("${batch.estate-rent.max-pages:0}")
    private int maxPages;

    @Value("${batch.estate-rent.max-items:0}")
    private int maxItems;

    public EstateRentCollectTasklet(EstateRentBatchDao estateRentBatchDao) {
        this.estateRentBatchDao = estateRentBatchDao;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        estateRentBatchDao.createHouseInfoTable();
        estateRentBatchDao.createHouseRentsTable();
        estateRentBatchDao.createReportTable();
        estateRentBatchDao.createItemTable();

        List<String> targetLawdCodes = resolveLawdCodes();
        List<String> targetDealYmds = resolveDealYmds();
        String reportId = "RENT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        List<EstateRentItem> items = collectAllFromOpenApi(targetLawdCodes, targetDealYmds);
        for (EstateRentItem item : items) {
            item.setReportId(reportId);
        }

        EstateRentReport report = buildReport(reportId, targetLawdCodes, targetDealYmds, items);
        estateRentBatchDao.insertReport(report);
        for (int start = 0; start < items.size(); start += ITEM_INSERT_CHUNK_SIZE) {
            List<EstateRentItem> chunk = items.subList(start, Math.min(start + ITEM_INSERT_CHUNK_SIZE, items.size()));
            estateRentBatchDao.insertItems(chunk);
        }

        // Spring Batch ExecutionContext에는 대량 거래 목록이 아니라 reportId만 둔다.
        var context = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        context.put(BatchContextKeys.RENT_REPORT_ID, reportId);

        System.out.println("[EstateRentBatch-Collect] reportId=" + reportId
                + ", lawdCodes=" + targetLawdCodes.size()
                + ", months=" + targetDealYmds
                + ", items=" + items.size());
        return RepeatStatus.FINISHED;
    }

    private List<EstateRentItem> collectAllFromOpenApi(List<String> targetLawdCodes, List<String> targetDealYmds) {
        if (isBlank(apiKey)) {
            System.out.println("[EstateRentBatch-Collect] ESTATE_API_KEY가 없어 전월세 수집을 건너뜁니다.");
            return List.of();
        }

        List<EstateRentItem> items = new ArrayList<>();
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

    private List<EstateRentItem> collectPagesFromOpenApi(String targetLawdCd, String targetDealYmd) {
        List<EstateRentItem> items = new ArrayList<>();
        for (int pageNo = 1; maxPages <= 0 || pageNo <= maxPages; pageNo++) {
            List<EstateRentItem> pageItems = collectOpenApiPage(targetLawdCd, targetDealYmd, pageNo);
            if (pageItems.isEmpty()) {
                break;
            }
            items.addAll(pageItems);
            if (pageItems.size() < pageSize || reachedMaxItems(items)) {
                break;
            }
        }
        return items;
    }

    private List<EstateRentItem> collectOpenApiPage(String targetLawdCd, String targetDealYmd, int pageNo) {
        try {
            // 설정 파일의 원본 키와 URL 인코딩된 키를 모두 허용한다.
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
            List<EstateRentItem> items = parseItems(xml, targetLawdCd, targetDealYmd);
            if (!items.isEmpty()) {
                System.out.println("[EstateRentBatch-Collect] OPEN_API lawdCd=" + targetLawdCd
                        + ", dealYmd=" + targetDealYmd + ", page=" + pageNo + ", items=" + items.size());
            }
            return items;
        } catch (Exception exception) {
            // 특정 시군구·월의 실패가 다른 수집 범위 전체를 막지 않도록 기록한 뒤 다음 범위로 진행한다.
            System.err.println("[EstateRentBatch-Collect] external API failed. lawdCd=" + targetLawdCd
                    + ", dealYmd=" + targetDealYmd + ", page=" + pageNo + ": " + exception.getMessage());
            return List.of();
        }
    }

    /* 같은 패키지의 단위 테스트에서 공공데이터 XML 필드 매핑을 검증한다. */
    List<EstateRentItem> parseItems(String xml, String targetLawdCd, String targetDealYmd) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        var document = builder.parse(new InputSource(new StringReader(xml)));
        NodeList nodes = document.getElementsByTagName("item");

        List<EstateRentItem> items = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            EstateRentItem item = new EstateRentItem();
            item.setSourceType("OPEN_API");
            item.setLawdCd(targetLawdCd);
            item.setDealYmd(targetDealYmd);
            item.setAptName(firstText(node, "aptNm", "aptName", "아파트"));
            item.setDong(firstText(node, "umdNm", "법정동"));
            item.setJibun(firstText(node, "jibun", "지번"));
            item.setDeposit(parseInt(firstText(node, "deposit", "보증금액")));
            item.setMonthlyRent(parseInt(firstText(node, "monthlyRent", "월세금액")));
            item.setArea(parseDouble(firstText(node, "excluUseAr", "전용면적")));
            item.setFloor(parseInt(firstText(node, "floor", "층")));
            item.setBuildYear(parseInt(firstText(node, "buildYear", "건축년도")));
            item.setDealDate(toDealDate(firstText(node, "dealYear", "년"),
                    firstText(node, "dealMonth", "월"), firstText(node, "dealDay", "일")));
            item.setContractTerm(firstText(node, "contractTerm", "계약기간"));
            item.setContractType(firstText(node, "contractType", "계약구분"));
            item.setUseRrRight(firstText(node, "useRRRight", "갱신요구권사용"));
            item.setPreDeposit(parseNullableInt(firstText(node, "preDeposit", "종전계약보증금")));
            item.setPreMonthlyRent(parseNullableInt(firstText(node, "preMonthlyRent", "종전계약월세")));
            if (!isBlank(item.getAptName())) {
                items.add(item);
            }
        }
        return items;
    }

    private EstateRentReport buildReport(String reportId, List<String> lawdCodes,
            List<String> dealYmds, List<EstateRentItem> items) {
        EstateRentReport report = new EstateRentReport();
        report.setReportId(reportId);
        report.setLawdCd(lawdCodes.size() == 1 ? lawdCodes.get(0) : "ALL");
        report.setDealYmd(String.join(",", dealYmds));
        report.setCollectedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.setItemCount(items.size());
        report.setJeonseCount((int) items.stream().filter(item -> item.getMonthlyRent() == 0).count());
        report.setMonthlyRentCount((int) items.stream().filter(item -> item.getMonthlyRent() > 0).count());
        return report;
    }

    private List<String> resolveLawdCodes() {
        if (!isBlank(sidoName)) {
            return estateRentBatchDao.selectLawdCodesBySido(sidoName.trim());
        }
        if ("ALL".equalsIgnoreCase(lawdCodes.trim())) {
            return estateRentBatchDao.selectAllLawdCodes();
        }
        return !isBlank(lawdCodes) ? splitCsv(lawdCodes) : splitCsv(lawdCd);
    }

    private List<String> resolveDealYmds() {
        if (!isBlank(configuredDealYmd)) {
            return splitCsv(configuredDealYmd);
        }
        List<String> dealYmds = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(1);
        for (int i = 0; i < Math.max(1, months); i++) {
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

    private boolean reachedMaxItems(List<EstateRentItem> items) {
        return maxItems > 0 && items.size() >= maxItems;
    }

    private String normalizeServiceKey(String configuredKey) {
        return configuredKey.contains("%")
                ? URLDecoder.decode(configuredKey, StandardCharsets.UTF_8)
                : configuredKey;
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

    private Integer parseNullableInt(String value) {
        return isBlank(value) ? null : parseInt(value);
    }

    private int parseInt(String value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
