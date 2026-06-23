package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.dto.RealEstateTransaction;

public interface HouseInfoService {

    List<HouseInfo> search(String sigunguCode, String dongName, String keyword,
            Integer maxPrice, String dealType, String housingType,
            Integer dealAmountMin, Integer dealAmountMax,
            Integer depositMin, Integer depositMax,
            Integer monthlyRentMin, Integer monthlyRentMax) throws Exception;

    HouseInfo findByAptSeq(String aptSeq) throws Exception;

    HouseInfo update(String aptSeq, String aptName, Integer buildYear, String latitude, String longitude) throws Exception;

    List<RealEstateTransaction> findRecentDeals(String aptSeq, int limit) throws Exception;
}
