package com.ssafy.prj.batch.bus;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 서울 버스 정류소 위치 API를 읽어 추천용 "법정동별 정류장 수"만 저장하는 배치다.
 *
 * <p>원본 정류소명·노선 정보는 저장하지 않는다. 좌표를 법정동으로 변환한 최소 캐시만 별도 보관해,
 * 다음 배치에서 신규·좌표 변경 정류소만 카카오 API로 보강한다.</p>
 */
@Component
public class BusStopMetricsCollectTasklet implements Tasklet {

    private static final ZoneId SEOUL_TIME_ZONE = ZoneId.of("Asia/Seoul");

    private final BusStopMetricsBatchDao busStopMetricsBatchDao;
    private final BusStopApiResponseParser responseParser;
    private final BusStopLegalDongResolver legalDongResolver;
    private final RestClient restClient = RestClient.create();

    /** 서울 Open API 기본 주소. 인증키와 XML·서비스·행 범위는 요청 시 path segment로 붙인다. */
    @Value("${batch.bus.api-url}")
    private String apiUrl;

    /** application-secret.properties의 SEOUL_OPEN_API_KEY를 사용한다. */
    @Value("${batch.bus.api-key:}")
    private String apiKey;

    /** 서울 Open API의 최대 요청 행 수에 맞춘 페이지 크기다. */
    @Value("${batch.bus.page-size:1000}")
    private int pageSize;

    /** 12개 내외의 페이지 요청이 연속으로 몰리지 않게 하는 대기 시간이다. */
    @Value("${batch.bus.request-delay-ms:100}")
    private long requestDelayMillis;

    public BusStopMetricsCollectTasklet(
        BusStopMetricsBatchDao busStopMetricsBatchDao,
        BusStopApiResponseParser responseParser,
        BusStopLegalDongResolver legalDongResolver
    ) {
        this.busStopMetricsBatchDao = busStopMetricsBatchDao;
        this.responseParser = responseParser;
        this.legalDongResolver = legalDongResolver;
    }

    @Override
    public org.springframework.batch.repeat.RepeatStatus execute(
        StepContribution contribution,
        ChunkContext chunkContext
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("batch.bus.api-key is required");
        }
        if (pageSize < 1 || pageSize > 1000) {
            throw new IllegalStateException("batch.bus.page-size must be between 1 and 1000");
        }

        busStopMetricsBatchDao.createMetricTable();
        busStopMetricsBatchDao.createDongCacheTable();

        List<BusStopCoordinate> busStops = collectAllBusStops();
        Map<String, BusStopCacheEntry> cachedByStopsNo = loadCachedStopsById();
        ResolvedMetrics resolved = resolveDongCodes(busStops, cachedByStopsNo);

        // 하나라도 법정동을 확정하지 못하면 기존 집계를 교체하지 않는다.
        if (!resolved.unresolvedStops().isEmpty()) {
            throw new IllegalStateException("Bus stop legal dong mapping is incomplete. unresolved="
                + resolved.unresolvedStops().size() + ", examples="
                + String.join(", ", resolved.unresolvedStops().stream().limit(10).toList()));
        }

        Map<String, Integer> countsByDongCode = filterToExistingDongCodes(resolved.countsByDongCode());
        if (countsByDongCode.isEmpty() && !busStops.isEmpty()) {
            throw new IllegalStateException("No bus stop legal dong code matched project dongcodes");
        }
        if (countsByDongCode.size() != resolved.countsByDongCode().size()) {
            throw new IllegalStateException("Bus stop legal dong code is missing from project dongcodes");
        }

        LocalDate sourceDate = LocalDate.now(SEOUL_TIME_ZONE);
        List<DongBusStopMetric> metrics = countsByDongCode.entrySet().stream()
            .map(entry -> new DongBusStopMetric(entry.getKey(), entry.getValue(), sourceDate))
            .toList();

        // 모든 페이지 수집·법정동 변환이 끝난 뒤에만 캐시와 집계를 교체한다.
        if (!resolved.cacheEntries().isEmpty()) {
            busStopMetricsBatchDao.upsertDongCaches(resolved.cacheEntries());
        }
        busStopMetricsBatchDao.deleteAllMetrics();
        if (!metrics.isEmpty()) {
            busStopMetricsBatchDao.insertMetrics(metrics);
        }

        System.out.println("[BusStopMetricsBatch] totalStops=" + busStops.size()
            + ", cachedMappings=" + resolved.cachedMappingCount()
            + ", newOrChangedMappings=" + resolved.newOrChangedMappingCount()
            + ", storedDongs=" + metrics.size()
            + ", sourceDate=" + sourceDate);
        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
    }

    /** API의 list_total_count를 기준으로 START_INDEX/END_INDEX 범위를 1,000건씩 이동한다. */
    private List<BusStopCoordinate> collectAllBusStops() throws Exception {
        Map<String, BusStopCoordinate> stopsById = new LinkedHashMap<>();
        int totalCount = -1;
        int startIndex = 1;

        while (totalCount < 0 || startIndex <= totalCount) {
            int endIndex = totalCount < 0
                ? pageSize
                : Math.min(startIndex + pageSize - 1, totalCount);
            BusStopApiPage page = requestPage(startIndex, endIndex);

            if (totalCount < 0) {
                totalCount = page.totalCount();
            }
            for (BusStopCoordinate busStop : page.stops()) {
                BusStopCoordinate previous = stopsById.putIfAbsent(busStop.stopsNo(), busStop);
                if (previous != null && !sameLocation(previous, busStop)) {
                    throw new IllegalStateException("Bus stop API returned conflicting coordinates. stopsNo="
                        + busStop.stopsNo());
                }
            }

            int pageNo = ((startIndex - 1) / pageSize) + 1;
            int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
            System.out.println("[BusStopMetricsBatch] page=" + pageNo
                + ", totalPages=" + totalPages
                + ", uniqueStops=" + stopsById.size());

            startIndex += pageSize;
            sleepBetweenPageRequests();
        }

        if (totalCount > 0 && stopsById.isEmpty()) {
            throw new IllegalStateException("Bus stop API returned total count but no rows");
        }
        return new ArrayList<>(stopsById.values());
    }

    /** 서울 Open API는 인증키를 path segment에 포함하는 형식이라 query parameter로 보내지 않는다. */
    private BusStopApiPage requestPage(int startIndex, int endIndex) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
            .pathSegment(apiKey, "xml", "busStopLocationXyInfo", String.valueOf(startIndex), String.valueOf(endIndex))
            .build()
            .encode()
            .toUri();

        String response = restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .body(String.class);
        return responseParser.parse(response);
    }

    /** 캐시에서 정류소번호를 키로 바로 찾을 수 있게 변환한다. */
    private Map<String, BusStopCacheEntry> loadCachedStopsById() {
        Map<String, BusStopCacheEntry> result = new HashMap<>();
        for (BusStopCacheEntry cache : busStopMetricsBatchDao.findAllCachedStops()) {
            result.put(cache.getStopsNo(), cache);
        }
        return result;
    }

    /**
     * 기존 캐시의 좌표와 같으면 법정동 코드를 재사용하고, 신규·좌표 변경 정류소만 역지오코딩한다.
     * 역지오코딩 결과가 하나라도 비면 부정확한 부분 집계를 저장하지 않도록 실행을 실패시킨다.
     */
    private ResolvedMetrics resolveDongCodes(
        List<BusStopCoordinate> busStops,
        Map<String, BusStopCacheEntry> cachedByStopsNo
    ) throws Exception {
        Map<String, Integer> countsByDongCode = new HashMap<>();
        List<BusStopCacheEntry> cacheEntries = new ArrayList<>(busStops.size());
        List<String> unresolvedStops = new ArrayList<>();
        int cachedMappingCount = 0;
        int newOrChangedMappingCount = 0;
        int processedCount = 0;

        for (BusStopCoordinate busStop : busStops) {
            BusStopCacheEntry cached = cachedByStopsNo.get(busStop.stopsNo());
            String dongCode;

            if (isReusableCache(cached, busStop)) {
                dongCode = cached.getDongCode();
                cachedMappingCount++;
            } else {
                dongCode = legalDongResolver.resolveLegalDongCode(busStop.latitude(), busStop.longitude());
                newOrChangedMappingCount++;
            }

            if (dongCode == null || dongCode.length() != 10) {
                unresolvedStops.add(busStop.stopsNo());
            } else {
                countsByDongCode.merge(dongCode, 1, Integer::sum);
                cacheEntries.add(new BusStopCacheEntry(
                    busStop.stopsNo(),
                    busStop.latitude(),
                    busStop.longitude(),
                    dongCode
                ));
            }

            processedCount++;
            logDongResolutionProgress(
                processedCount,
                busStops.size(),
                cachedMappingCount,
                newOrChangedMappingCount,
                unresolvedStops.size()
            );
        }

        return new ResolvedMetrics(
            countsByDongCode,
            cacheEntries,
            unresolvedStops,
            cachedMappingCount,
            newOrChangedMappingCount
        );
    }

    /** 최초 실행처럼 역지오코딩이 오래 걸리는 경우에도 현재 진행 상태를 콘솔에서 확인하게 한다. */
    private void logDongResolutionProgress(
        int processedCount,
        int totalCount,
        int cachedMappingCount,
        int newOrChangedMappingCount,
        int unresolvedCount
    ) {
        if (processedCount % 250 != 0 && processedCount != totalCount) {
            return;
        }
        System.out.println("[BusStopMetricsBatch-Geocoding] processed=" + processedCount + "/" + totalCount
            + ", cached=" + cachedMappingCount
            + ", requested=" + newOrChangedMappingCount
            + ", unresolved=" + unresolvedCount);
    }

    /** 외부 API의 코드가 프로젝트 dongcodes에 있어야만 FK를 만족하는 통계를 만들 수 있다. */
    private Map<String, Integer> filterToExistingDongCodes(Map<String, Integer> countsByDongCode) {
        if (countsByDongCode.isEmpty()) {
            return Map.of();
        }
        Set<String> existingCodes = new HashSet<>(busStopMetricsBatchDao.findExistingDongCodes(
            new ArrayList<>(countsByDongCode.keySet())
        ));
        Map<String, Integer> filtered = new HashMap<>();
        for (Map.Entry<String, Integer> entry : countsByDongCode.entrySet()) {
            if (existingCodes.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private boolean isReusableCache(BusStopCacheEntry cache, BusStopCoordinate busStop) {
        return cache != null
            && cache.getDongCode() != null
            && cache.getDongCode().length() == 10
            && sameCoordinate(cache.getLatitude(), busStop.latitude())
            && sameCoordinate(cache.getLongitude(), busStop.longitude());
    }

    private boolean sameLocation(BusStopCoordinate first, BusStopCoordinate second) {
        return sameCoordinate(first.latitude(), second.latitude())
            && sameCoordinate(first.longitude(), second.longitude());
    }

    /** BigDecimal은 scale이 달라도 좌표 값이 같으면 같은 위치로 본다. */
    private boolean sameCoordinate(BigDecimal first, BigDecimal second) {
        return first != null && second != null && first.compareTo(second) == 0;
    }

    private void sleepBetweenPageRequests() {
        if (requestDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(requestDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record ResolvedMetrics(
        Map<String, Integer> countsByDongCode,
        List<BusStopCacheEntry> cacheEntries,
        List<String> unresolvedStops,
        int cachedMappingCount,
        int newOrChangedMappingCount
    ) {
    }
}
