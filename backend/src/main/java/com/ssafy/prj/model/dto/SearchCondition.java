package com.ssafy.prj.model.dto;

public class SearchCondition {

    private String housingType;
    private String contractType;
    private String rentType;
    private String dealYmd;
    private String lawdCd;
    private String dong;
    private String keyword;
    private Integer dealAmountMin;
    private Integer dealAmountMax;
    private Integer depositMin;
    private Integer depositMax;
    private Integer monthlyRentMin;
    private Integer monthlyRentMax;

    public String getHousingType() {
        return housingType;
    }

    public void setHousingType(String housingType) {
        this.housingType = housingType;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    /** 전월세 안에서 전체·전세·월세를 구분한다. all, jeonse, monthly 값을 사용한다. */
    public String getRentType() {
        return rentType;
    }

    public void setRentType(String rentType) {
        this.rentType = rentType;
    }

    public String getDealYmd() {
        return dealYmd;
    }

    public void setDealYmd(String dealYmd) {
        this.dealYmd = dealYmd;
    }

    public String getLawdCd() {
        return lawdCd;
    }

    public void setLawdCd(String lawdCd) {
        this.lawdCd = lawdCd;
    }

    public String getDong() {
        return dong;
    }

    public void setDong(String dong) {
        this.dong = dong;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /** 매매가 범위다. 전월세에는 적용하지 않는다. 단위는 만원이다. */
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

    /** 전세·월세 보증금 범위다. 단위는 만원이다. */
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

    /** 월세 범위다. monthlyRent가 0인 전세에는 적용하지 않는다. 단위는 만원이다. */
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
}
