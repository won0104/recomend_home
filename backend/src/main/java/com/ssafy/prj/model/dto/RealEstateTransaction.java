package com.ssafy.prj.model.dto;

import java.io.Serializable;

public class RealEstateTransaction implements Serializable {

    private String id;
    private String aptSeq;
    private String housingType;
    private String contractType;
    private String name;
    private String dong;
    private String lot;
    private String regionCode;
    private String sido;
    private String sigungu;
    private String emd;
    private int amount;
    private int deposit;
    private int monthlyRent;
    private double area;
    private int floor;
    private int builtYear;
    private String date;
    private String dealYmd;
    private String address;

    public RealEstateTransaction() {
    }

    public RealEstateTransaction(String id, String housingType, String contractType, String name, String dong, String lot,
            String regionCode, String sido, String sigungu, String emd, int amount, int deposit, int monthlyRent,
            double area, int floor, int builtYear, String date, String dealYmd, String address) {
        this.id = id;
        this.housingType = housingType;
        this.contractType = contractType;
        this.name = name;
        this.dong = dong;
        this.lot = lot;
        this.regionCode = regionCode;
        this.sido = sido;
        this.sigungu = sigungu;
        this.emd = emd;
        this.amount = amount;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.area = area;
        this.floor = floor;
        this.builtYear = builtYear;
        this.date = date;
        this.dealYmd = dealYmd;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAptSeq() {
        return aptSeq;
    }

    public void setAptSeq(String aptSeq) {
        this.aptSeq = aptSeq;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDong() {
        return dong;
    }

    public void setDong(String dong) {
        this.dong = dong;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getSido() {
        return sido;
    }

    public void setSido(String sido) {
        this.sido = sido;
    }

    public String getSigungu() {
        return sigungu;
    }

    public void setSigungu(String sigungu) {
        this.sigungu = sigungu;
    }

    public String getEmd() {
        return emd;
    }

    public void setEmd(String emd) {
        this.emd = emd;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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

    public int getBuiltYear() {
        return builtYear;
    }

    public void setBuiltYear(int builtYear) {
        this.builtYear = builtYear;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDealYmd() {
        return dealYmd;
    }

    public void setDealYmd(String dealYmd) {
        this.dealYmd = dealYmd;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
