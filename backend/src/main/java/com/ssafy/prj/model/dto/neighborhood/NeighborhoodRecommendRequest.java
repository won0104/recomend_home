package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendRequest {

    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    /** 이전 화면과의 호환을 위한 매매 최대가 필드다. 새 요청은 dealAmountMax를 사용한다. */
    private Integer maxPrice;
    /** 화면에서 선택한 거래 유형: 매매, 전세, 월세. */
    private String dealType = "매매";
    /** 매매가 범위(만원). 매매 후보를 만들 때만 사용한다. */
    private Integer dealAmountMin;
    private Integer dealAmountMax;
    /** 보증금 범위(만원). 전세·월세 후보를 만들 때 사용한다. */
    private Integer depositMin;
    private Integer depositMax;
    /** 월세 범위(만원). 월세 후보를 만들 때만 사용한다. */
    private Integer monthlyRentMin;
    private Integer monthlyRentMax;
    /** 화면에서 선택한 주거 형태. 현재 원천은 아파트 중심이므로 선택값만 보존한다. */
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

    public Integer getDealAmountMin() {
        return dealAmountMin;
    }

    public void setDealAmountMin(Integer dealAmountMin) {
        this.dealAmountMin = dealAmountMin;
    }

    public Integer getDealAmountMax() {
        return dealAmountMax;
    }

    public void setDealAmountMax(Integer dealAmountMax) {
        this.dealAmountMax = dealAmountMax;
    }

    public Integer getDepositMin() {
        return depositMin;
    }

    public void setDepositMin(Integer depositMin) {
        this.depositMin = depositMin;
    }

    public Integer getDepositMax() {
        return depositMax;
    }

    public void setDepositMax(Integer depositMax) {
        this.depositMax = depositMax;
    }

    public Integer getMonthlyRentMin() {
        return monthlyRentMin;
    }

    public void setMonthlyRentMin(Integer monthlyRentMin) {
        this.monthlyRentMin = monthlyRentMin;
    }

    public Integer getMonthlyRentMax() {
        return monthlyRentMax;
    }

    public void setMonthlyRentMax(Integer monthlyRentMax) {
        this.monthlyRentMax = monthlyRentMax;
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
