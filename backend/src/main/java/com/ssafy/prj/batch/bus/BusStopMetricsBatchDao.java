package com.ssafy.prj.batch.bus;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 버스 정류소 법정동 캐시와 추천용 동별 집계를 관리한다. */
@Mapper
public interface BusStopMetricsBatchDao {

    /** 처음 실행해도 별도 SQL 실행 없이 배치 전용 테이블을 사용할 수 있게 만든다. */
    void createMetricTable();

    /** 좌표→법정동 변환 결과를 보관할 최소 캐시 테이블을 만든다. */
    void createDongCacheTable();

    /** 기존 캐시 전체를 읽어 새 정류장 또는 위치 변경 정류장만 역지오코딩한다. */
    List<BusStopCacheEntry> findAllCachedStops();

    /** 카카오 API가 반환한 코드도 프로젝트 dongcodes에 실제로 있을 때만 FK로 저장한다. */
    List<String> findExistingDongCodes(@Param("dongCodes") List<String> dongCodes);

    /** 원본 정류장 전체가 아니라 정류장 식별자·좌표·법정동 코드만 갱신한다. */
    int upsertDongCaches(@Param("caches") List<BusStopCacheEntry> caches);

    /** 새 스냅샷이 완성된 경우에만 이전 법정동별 정류장 수를 비운다. */
    int deleteAllMetrics();

    /** 한 번의 다중 INSERT로 새 법정동별 정류장 수 스냅샷을 저장한다. */
    int insertMetrics(@Param("metrics") List<DongBusStopMetric> metrics);
}
