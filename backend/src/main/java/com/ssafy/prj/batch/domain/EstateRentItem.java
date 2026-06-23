package com.ssafy.prj.batch.domain;

import java.io.Serializable;

/** 공공데이터포털 아파트 전월세 API에서 읽은 거래 한 건이다. */
public class EstateRentItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;
    private String sourceType;
    private String lawdCd;
    private String dealYmd;
    private String aptName;
    private String dong;
    private String jibun;
    private int deposit;
    private int monthlyRent;
    private double area;
    private int floor;
    private int buildYear;
    private String dealDate;
    private String contractTerm;
    private String contractType;
    private String useRrRight;
    private Integer preDeposit;
    private Integer preMonthlyRent;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getLawdCd() { return lawdCd; }
    public void setLawdCd(String lawdCd) { this.lawdCd = lawdCd; }
    public String getDealYmd() { return dealYmd; }
    public void setDealYmd(String dealYmd) { this.dealYmd = dealYmd; }
    public String getAptName() { return aptName; }
    public void setAptName(String aptName) { this.aptName = aptName; }
    public String getDong() { return dong; }
    public void setDong(String dong) { this.dong = dong; }
    public String getJibun() { return jibun; }
    public void setJibun(String jibun) { this.jibun = jibun; }
    public int getDeposit() { return deposit; }
    public void setDeposit(int deposit) { this.deposit = deposit; }
    public int getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(int monthlyRent) { this.monthlyRent = monthlyRent; }
    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public int getBuildYear() { return buildYear; }
    public void setBuildYear(int buildYear) { this.buildYear = buildYear; }
    public String getDealDate() { return dealDate; }
    public void setDealDate(String dealDate) { this.dealDate = dealDate; }
    public String getContractTerm() { return contractTerm; }
    public void setContractTerm(String contractTerm) { this.contractTerm = contractTerm; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getUseRrRight() { return useRrRight; }
    public void setUseRrRight(String useRrRight) { this.useRrRight = useRrRight; }
    public Integer getPreDeposit() { return preDeposit; }
    public void setPreDeposit(Integer preDeposit) { this.preDeposit = preDeposit; }
    public Integer getPreMonthlyRent() { return preMonthlyRent; }
    public void setPreMonthlyRent(Integer preMonthlyRent) { this.preMonthlyRent = preMonthlyRent; }
}
