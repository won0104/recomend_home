package com.ssafy.prj.model.dto;

public class TransactionSummary {

    private int totalCount;
    private int dealCount;
    private int rentCount;
    private int avgDealAmount;

    public TransactionSummary() {
    }

    public TransactionSummary(int totalCount, int dealCount, int rentCount, int avgDealAmount) {
        this.totalCount = totalCount;
        this.dealCount = dealCount;
        this.rentCount = rentCount;
        this.avgDealAmount = avgDealAmount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getDealCount() {
        return dealCount;
    }

    public void setDealCount(int dealCount) {
        this.dealCount = dealCount;
    }

    public int getRentCount() {
        return rentCount;
    }

    public void setRentCount(int rentCount) {
        this.rentCount = rentCount;
    }

    public int getAvgDealAmount() {
        return avgDealAmount;
    }

    public void setAvgDealAmount(int avgDealAmount) {
        this.avgDealAmount = avgDealAmount;
    }
}
