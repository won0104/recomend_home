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
    public List<HouseInfo> search(String sigunguCode, String dongName, String keyword) throws Exception {
        return houseInfoMapper.search(sigunguCode, dongName, keyword);
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
    public List<RealEstateTransaction> findRecentDeals(String aptSeq, int limit) throws Exception {
        if (isBlank(aptSeq)) {
            return Collections.emptyList();
        }
        return houseInfoMapper.findRecentDealsByAptSeq(aptSeq, Math.max(1, Math.min(limit, 100)));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
