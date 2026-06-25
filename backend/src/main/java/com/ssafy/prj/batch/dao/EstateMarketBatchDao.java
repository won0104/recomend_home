package com.ssafy.prj.batch.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;
import com.ssafy.prj.batch.domain.HouseCoordinateTarget;

@Mapper
public interface EstateMarketBatchDao {

    void createReportTable();

    void createHouseInfoTable();

    void createHouseDealsTable();

    void widenReportDealYmdColumn();

    void createItemTable();

    int insertReport(EstateMarketReport report);

    int insertItems(@Param("items") List<EstateMarketItem> items);

    EstateMarketReport findReportById(@Param("reportId") String reportId);

    List<EstateMarketItem> findItemsByReportId(@Param("reportId") String reportId);

    int upsertHouseInfos(@Param("items") List<EstateMarketItem> items);

    int insertNewHouseDeals(@Param("items") List<EstateMarketItem> items);

    int normalizeLegalDongCodesForReport(@Param("reportId") String reportId);

    List<HouseCoordinateTarget> findMissingCoordinateTargetsByReportId(@Param("reportId") String reportId);

    int updateHouseCoordinates(@Param("aptSeq") String aptSeq,
            @Param("latitude") String latitude,
            @Param("longitude") String longitude);

    List<String> selectAllLawdCodes();

    List<String> selectLawdCodesBySido(@Param("sidoName") String sidoName);

    List<EstateMarketItem> selectFallbackItems(@Param("lawdCd") String lawdCd,
            @Param("dealYmd") String dealYmd,
            @Param("limit") int limit);
}
