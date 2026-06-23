package com.ssafy.prj.model.dto;

/**
 * 아파트 전월세 실거래 한 건이다.
 *
 * <p>매매 가격과 전월세의 보증금·월세는 의미가 달라 housedeals와 분리한다.
 * {@code monthlyRent == 0}이면 전세, 0보다 크면 월세로 판단한다.</p>
 */
public class HouseRent {

    private Long rentId;
    private String aptSeq;
    private String floor;
    private int dealYear;
    private int dealMonth;
    private int dealDay;
    private double excluUseAr;
    private int deposit;
    private int monthlyRent;
    private String contractTerm;
    private String contractType;
    private String useRrRight;
    private Integer preDeposit;
    private Integer preMonthlyRent;
    private String sourceHash;

    public Long getRentId() {
        return rentId;
    }

    public void setRentId(Long rentId) {
        this.rentId = rentId;
    }

    public String getAptSeq() {
        return aptSeq;
    }

    public void setAptSeq(String aptSeq) {
        this.aptSeq = aptSeq;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public int getDealYear() {
        return dealYear;
    }

    public void setDealYear(int dealYear) {
        this.dealYear = dealYear;
    }

    public int getDealMonth() {
        return dealMonth;
    }

    public void setDealMonth(int dealMonth) {
        this.dealMonth = dealMonth;
    }

    public int getDealDay() {
        return dealDay;
    }

    public void setDealDay(int dealDay) {
        this.dealDay = dealDay;
    }

    public double getExcluUseAr() {
        return excluUseAr;
    }

    public void setExcluUseAr(double excluUseAr) {
        this.excluUseAr = excluUseAr;
    }

    public int getDeposit() {
        return deposit;
    }

    public void setDeposit(int deposit) {
        this.deposit = deposit;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(int monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getContractTerm() {
        return contractTerm;
    }

    public void setContractTerm(String contractTerm) {
        this.contractTerm = contractTerm;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getUseRrRight() {
        return useRrRight;
    }

    public void setUseRrRight(String useRrRight) {
        this.useRrRight = useRrRight;
    }

    public Integer getPreDeposit() {
        return preDeposit;
    }

    public void setPreDeposit(Integer preDeposit) {
        this.preDeposit = preDeposit;
    }

    public Integer getPreMonthlyRent() {
        return preMonthlyRent;
    }

    public void setPreMonthlyRent(Integer preMonthlyRent) {
        this.preMonthlyRent = preMonthlyRent;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }
}
