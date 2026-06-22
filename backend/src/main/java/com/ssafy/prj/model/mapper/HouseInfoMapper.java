package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.dto.RealEstateTransaction;

@Mapper
public interface HouseInfoMapper {

    List<HouseInfo> search(@Param("sigunguCode") String sigunguCode,
            @Param("dongName") String dongName,
            @Param("keyword") String keyword);

    HouseInfo findByAptSeq(String aptSeq);

    int update(HouseInfo houseInfo);

    List<RealEstateTransaction> findRecentDealsByAptSeq(@Param("aptSeq") String aptSeq, @Param("limit") int limit);
}
