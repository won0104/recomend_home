package com.ssafy.prj.batch.domain;

import java.io.Serializable;

/** 전월세 수집 한 번의 범위와 결과 건수를 보관하는 배치 리포트다. */
public class EstateRentReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;
    private String lawdCd;
    private String dealYmd;
    private String collectedAt;
    private int itemCount;
    private int jeonseCount;
    private int monthlyRentCount;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getLawdCd() { return lawdCd; }
    public void setLawdCd(String lawdCd) { this.lawdCd = lawdCd; }
    public String getDealYmd() { return dealYmd; }
    public void setDealYmd(String dealYmd) { this.dealYmd = dealYmd; }
    public String getCollectedAt() { return collectedAt; }
    public void setCollectedAt(String collectedAt) { this.collectedAt = collectedAt; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public int getJeonseCount() { return jeonseCount; }
    public void setJeonseCount(int jeonseCount) { this.jeonseCount = jeonseCount; }
    public int getMonthlyRentCount() { return monthlyRentCount; }
    public void setMonthlyRentCount(int monthlyRentCount) { this.monthlyRentCount = monthlyRentCount; }
}
