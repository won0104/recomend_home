package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.dto.RealEstateTransaction;

@Mapper
public interface HouseInfoMapper {

    /**
     * 법정동과 거래 유형별 예산 범위로 상세 매물(아파트) 목록을 조회한다.
     */
    List<HouseInfo> search(@Param("sigunguCode") String sigunguCode,
            @Param("dongName") String dongName,
            @Param("keyword") String keyword,
            @Param("maxPrice") Integer maxPrice,
            @Param("dealType") String dealType,
            @Param("housingType") String housingType,
            @Param("dealAmountMin") Integer dealAmountMin,
            @Param("dealAmountMax") Integer dealAmountMax,
            @Param("depositMin") Integer depositMin,
            @Param("depositMax") Integer depositMax,
            @Param("monthlyRentMin") Integer monthlyRentMin,
            @Param("monthlyRentMax") Integer monthlyRentMax);

    HouseInfo findByAptSeq(String aptSeq);

    int update(HouseInfo houseInfo);

    List<RealEstateTransaction> findRecentDealsByAptSeq(@Param("aptSeq") String aptSeq, @Param("limit") int limit);
}
