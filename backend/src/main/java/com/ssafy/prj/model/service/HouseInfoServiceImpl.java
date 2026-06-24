package com.ssafy.prj.model.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.HouseInfo;
import com.ssafy.prj.model.dto.RealEstateTransaction;
import com.ssafy.prj.model.mapper.HouseInfoMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseInfoServiceImpl implements HouseInfoService {

    private final HouseInfoMapper houseInfoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HouseInfo> search(String sigunguCode, String dongName, String keyword,
            String dealType, String housingType,
            Integer dealAmountMin, Integer dealAmountMax,
            Integer depositMin, Integer depositMax,
            Integer monthlyRentMin, Integer monthlyRentMax) throws Exception {
        // 동네 추천 후보를 만드는 조건과 동일하게, 상세 목록에서도 거래 유형별 금액 범위를 적용한다.
        return houseInfoMapper.search(sigunguCode, dongName, keyword, dealType, housingType,
                dealAmountMin, dealAmountMax, depositMin, depositMax, monthlyRentMin, monthlyRentMax);
    }

    @Override
    @Transactional(readOnly = true)
    public HouseInfo findByAptSeq(String aptSeq) throws Exception {
        if (isBlank(aptSeq)) {
            throw new IllegalArgumentException("아파트 식별자가 없습니다.");
        }
        return houseInfoMapper.findByAptSeq(aptSeq);
    }

    @Override
    @Transactional
    public HouseInfo update(String aptSeq, String aptName, Integer buildYear, String latitude, String longitude) throws Exception {
        if (isBlank(aptSeq)) {
            throw new IllegalArgumentException("아파트 식별자가 없습니다.");
        }
        if (isBlank(aptName)) {
            throw new IllegalArgumentException("아파트명은 필수입니다.");
        }

        HouseInfo existing = houseInfoMapper.findByAptSeq(aptSeq);
        if (existing == null) {
            throw new IllegalArgumentException("주택 정보를 찾을 수 없습니다.");
        }
        existing.setAptNm(aptName.trim());
        existing.setBuildYear(buildYear);
        existing.setLatitude(latitude);
        existing.setLongitude(longitude);
        houseInfoMapper.update(existing);
        return houseInfoMapper.findByAptSeq(aptSeq);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RealEstateTransaction> findRecentDeals(String aptSeq, String dealType, int limit) throws Exception {
        if (isBlank(aptSeq)) {
            return Collections.emptyList();
        }
        // 상세 링크가 거래 유형을 전달하지 않은 기존 화면은 매매 내역을 기본값으로 사용한다.
        String resolvedDealType = isBlank(dealType) ? "매매" : dealType.trim();
        if (!List.of("매매", "전세", "월세").contains(resolvedDealType)) {
            throw new IllegalArgumentException("거래 유형은 매매, 전세, 월세 중 하나여야 합니다.");
        }
        return houseInfoMapper.findRecentDealsByAptSeq(aptSeq, resolvedDealType,
                Math.max(1, Math.min(limit, 100)));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
