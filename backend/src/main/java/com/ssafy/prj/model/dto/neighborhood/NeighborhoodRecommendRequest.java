package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendRequest {

    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Integer maxPrice;
    /** 화면에서 선택한 거래 유형. 원천 데이터 확장 시 후보·상세 필터에 연결할 값이다. */
    private String dealType = "매매";
    /** 화면에서 선택한 주거 형태. 원천 데이터 확장 시 후보·상세 필터에 연결할 값이다. */
    private String housingType = "아파트";
    private List<String> priorities = new ArrayList<>();
    private String lifestyleMood;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getDealType() {
        return dealType;
    }

    public void setDealType(String dealType) {
        this.dealType = dealType;
    }

    public String getHousingType() {
        return housingType;
    }

    public void setHousingType(String housingType) {
        this.housingType = housingType;
    }

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities;
    }

    public String getLifestyleMood() {
        return lifestyleMood;
    }

    public void setLifestyleMood(String lifestyleMood) {
        this.lifestyleMood = lifestyleMood;
    }

}
