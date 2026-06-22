package com.ssafy.prj.model.dto;

public class SearchCondition {

    private String housingType;
    private String contractType;
    private String dealYmd;
    private String lawdCd;
    private String dong;
    private String keyword;

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
}
