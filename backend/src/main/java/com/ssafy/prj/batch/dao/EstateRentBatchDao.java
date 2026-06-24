package com.ssafy.prj.batch.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.batch.domain.EstateRentItem;
import com.ssafy.prj.batch.domain.EstateRentReport;
import com.ssafy.prj.batch.domain.HouseCoordinateTarget;

/** 전월세 배치가 사용하는 임시 적재·서비스 테이블 접근 지점이다. */
@Mapper
public interface EstateRentBatchDao {

    void createHouseInfoTable();
    void createHouseRentsTable();
    void createReportTable();
    void createItemTable();

    int insertReport(EstateRentReport report);
    int insertItems(@Param("items") List<EstateRentItem> items);
    EstateRentReport findReportById(@Param("reportId") String reportId);
    List<EstateRentItem> findItemsByReportId(@Param("reportId") String reportId);

    int upsertHouseInfos(@Param("items") List<EstateRentItem> items);
    int insertNewHouseRents(@Param("items") List<EstateRentItem> items);
    int normalizeLegalDongCodesForReport(@Param("reportId") String reportId);
    List<HouseCoordinateTarget> findMissingCoordinateTargetsByReportId(@Param("reportId") String reportId);
    int updateHouseCoordinates(@Param("aptSeq") String aptSeq,
            @Param("latitude") String latitude,
            @Param("longitude") String longitude);

    List<String> selectAllLawdCodes();
    List<String> selectLawdCodesBySido(@Param("sidoName") String sidoName);
}
