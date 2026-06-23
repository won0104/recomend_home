package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.dto.RealEstateTransaction;

@Mapper
public interface HouseInfoMapper {

    /**
     * 법정동·예산과 화면에서 선택한 주거 조건을 함께 받는 상세 매물 조회다.
     * 현재 SQL 필터는 예산에 적용하며, 거래 유형·주거 형태는 원천 데이터 확장 전까지 전달값만 유지한다.
     */
    List<HouseInfo> search(@Param("sigunguCode") String sigunguCode,
            @Param("dongName") String dongName,
            @Param("keyword") String keyword,
            @Param("maxPrice") Integer maxPrice,
            @Param("dealType") String dealType,
            @Param("housingType") String housingType);

    HouseInfo findByAptSeq(String aptSeq);

    int update(HouseInfo houseInfo);

    List<RealEstateTransaction> findRecentDealsByAptSeq(@Param("aptSeq") String aptSeq, @Param("limit") int limit);
}
