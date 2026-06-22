package com.ssafy.prj.batch.domain;

import java.io.Serializable;

public class EstateMarketReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;
    private String title;
    private String lawdCd;
    private String dealYmd;
    private String collectedAt;
    private int itemCount;
    private int avgDealAmount;
    private int maxDealAmount;
    private int minDealAmount;
    private String aiSummary;
    private String aiStatus;
    private String pdfPath;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(String collectedAt) {
        this.collectedAt = collectedAt;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getAvgDealAmount() {
        return avgDealAmount;
    }

    public void setAvgDealAmount(int avgDealAmount) {
        this.avgDealAmount = avgDealAmount;
    }

    public int getMaxDealAmount() {
        return maxDealAmount;
    }

    public void setMaxDealAmount(int maxDealAmount) {
        this.maxDealAmount = maxDealAmount;
    }

    public int getMinDealAmount() {
        return minDealAmount;
    }

    public void setMinDealAmount(int minDealAmount) {
        this.minDealAmount = minDealAmount;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(String aiStatus) {
        this.aiStatus = aiStatus;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }
}
