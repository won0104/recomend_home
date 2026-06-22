package com.ssafy.prj.batch.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;

@Mapper
public interface EstateMarketBatchDao {

    void createReportTable();

    void widenReportDealYmdColumn();

    void createItemTable();

    int insertReport(EstateMarketReport report);

    int insertItems(@Param("items") List<EstateMarketItem> items);

    int upsertHouseInfos(@Param("items") List<EstateMarketItem> items);

    int insertNewHouseDeals(@Param("items") List<EstateMarketItem> items);

    int updateAiResult(EstateMarketReport report);

    int updatePdfPath(EstateMarketReport report);

    List<String> selectAllLawdCodes();

    List<EstateMarketItem> selectFallbackItems(@Param("lawdCd") String lawdCd,
            @Param("dealYmd") String dealYmd,
            @Param("limit") int limit);
}
