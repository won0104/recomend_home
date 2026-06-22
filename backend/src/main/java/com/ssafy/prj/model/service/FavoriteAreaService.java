package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.FavoriteArea;

public interface FavoriteAreaService {

    List<FavoriteArea> findByUserId(String userId) throws Exception;

    FavoriteArea add(String userId, String key, String sido, String sigungu, String emd, String lawdCd) throws Exception;

    boolean delete(String userId, String key) throws Exception;

    void deleteAllByUserId(String userId) throws Exception;
}
