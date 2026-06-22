package com.ssafy.prj.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.FavoriteArea;
import com.ssafy.prj.model.mapper.FavoriteAreaMapper;
import com.ssafy.prj.util.TimeUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteAreaServiceImpl implements FavoriteAreaService {

    private final FavoriteAreaMapper favoriteAreaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteArea> findByUserId(String userId) throws Exception {
        return favoriteAreaMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public FavoriteArea add(String userId, String key, String sido, String sigungu, String emd, String lawdCd)
            throws Exception {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (isBlank(lawdCd)) {
            throw new IllegalArgumentException("법정동 코드는 필수입니다.");
        }

        String resolvedKey = isBlank(key) ? lawdCd : key;
        FavoriteArea favoriteArea = new FavoriteArea(resolvedKey, userId, sido, sigungu, emd, lawdCd, TimeUtil.now());

        int inserted = favoriteAreaMapper.insert(favoriteArea);
        if (inserted == 0) {
            throw new IllegalArgumentException("이미 등록된 관심지역입니다.");
        }
        return favoriteArea;
    }

    @Override
    @Transactional
    public boolean delete(String userId, String key) throws Exception {
        return favoriteAreaMapper.delete(userId, key) == 1;
    }

    @Override
    @Transactional
    public void deleteAllByUserId(String userId) throws Exception {
        favoriteAreaMapper.deleteByUserId(userId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
