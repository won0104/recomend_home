package com.ssafy.prj.chat.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.ssafy.prj.model.service.neighborhood.SurveyCategory;

public record ChatBotSlotDTO(
    @Description("사용자가 기준 위치로 말한 장소 키워드입니다. 예: 서울역, 삼성역, 홍대입구역, 강남구청")
    @JsonPropertyDescription("사용자가 기준 위치로 말한 장소 키워드입니다. 예: 서울역, 삼성역, 홍대입구역, 강남구청")
    String locationKeyword,

    @Description("기준 위치에서 추천 후보를 찾을 반경입니다. 단위는 km입니다.")
    @JsonPropertyDescription("기준 위치에서 추천 후보를 찾을 반경입니다. 단위는 km입니다.")
    Double radiusKm,

    @Description("거래 유형입니다. 매매, 전세, 월세 중 사용자가 말한 값을 넣습니다.")
    @JsonPropertyDescription("거래 유형입니다. 매매, 전세, 월세 중 사용자가 말한 값을 넣습니다.")
    String dealType,

    @Description("매매일 때의 최대 매매가입니다. 단위는 만원입니다. 예: 10억원은 100000")
    @JsonPropertyDescription("매매일 때의 최대 매매가입니다. 단위는 만원입니다. 예: 10억원은 100000")
    Long dealAmountMax,

    @Description("전세 또는 월세일 때의 최대 보증금입니다. 단위는 만원입니다. 예: 5억원은 50000, 5000만원은 5000")
    @JsonPropertyDescription("전세 또는 월세일 때의 최대 보증금입니다. 단위는 만원입니다. 예: 5억원은 50000, 5000만원은 5000")
    Long depositMax,

    @Description("월세일 때의 최대 월세입니다. 단위는 만원입니다. 예: 월세 100만원은 100")
    @JsonPropertyDescription("월세일 때의 최대 월세입니다. 단위는 만원입니다. 예: 월세 100만원은 100")
    Long monthlyRentMax,

    @Description("주거 형태입니다. 아파트, 원룸, 오피스텔, 빌라 등 사용자가 말한 값을 넣습니다.")
    @JsonPropertyDescription("주거 형태입니다. 아파트, 원룸, 오피스텔, 빌라 등 사용자가 말한 값을 넣습니다.")
    String housingType,

    @Description("사용자의 생활 우선순위 3개입니다. SCHOOL, CAFE, CULTURE, SHOPPING, PARK, MEDICAL, PET 중 정확히 3개입니다.")
    @JsonPropertyDescription("사용자의 생활 우선순위 3개입니다. SCHOOL, CAFE, CULTURE, SHOPPING, PARK, MEDICAL, PET 중 정확히 3개입니다.")
    List<SurveyCategory> priorities
) {
}
