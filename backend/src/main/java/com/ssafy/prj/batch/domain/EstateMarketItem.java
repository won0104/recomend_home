package com.ssafy.prj.batch.domain;

import java.io.Serializable;

public class EstateMarketItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;
    private String sourceType;
    private String lawdCd;
    private String dealYmd;
    private String aptName;
    private String dong;
    private String jibun;
    private int dealAmount;
    private double area;
    private int floor;
    private int buildYear;
    private String dealDate;
    private String rawAddress;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getLawdCd() {
        return lawdCd;
    }

    public void setLawdCd(String lawdCd) {
        this.lawdCd = lawdCd;
    }

    public String getDealYmd() {
        return dealYmd;
    }

    public void setDealYmd(String dealYmd) {
        this.dealYmd = dealYmd;
    }

    public String getAptName() {
        return aptName;
    }

    public void setAptName(String aptName) {
        this.aptName = aptName;
    }

    public String getDong() {
        return dong;
    }

    public void setDong(String dong) {
        this.dong = dong;
    }

    public String getJibun() {
        return jibun;
    }

    public void setJibun(String jibun) {
        this.jibun = jibun;
    }

    public int getDealAmount() {
        return dealAmount;
    }

    public void setDealAmount(int dealAmount) {
        this.dealAmount = dealAmount;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getBuildYear() {
        return buildYear;
    }

    public void setBuildYear(int buildYear) {
        this.buildYear = buildYear;
    }

    public String getDealDate() {
        return dealDate;
    }

    public void setDealDate(String dealDate) {
        this.dealDate = dealDate;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
    }
}
