package com.ssafy.prj.chat.service;

import com.ssafy.prj.chat.dto.KakaoPlace;

public interface KakaoApiService {

    KakaoPlace searchFirstPlace(String keyword);
}
