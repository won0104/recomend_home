package com.ssafy.prj.batch.bus;

import java.math.BigDecimal;

/** 정류소 좌표를 법정동 코드로 변환한 결과를 재사용하기 위한 최소 캐시 모델이다. */
public class BusStopCacheEntry {

    private String stopsNo;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String dongCode;

    public BusStopCacheEntry() {
    }

    public BusStopCacheEntry(String stopsNo, BigDecimal latitude, BigDecimal longitude, String dongCode) {
        this.stopsNo = stopsNo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dongCode = dongCode;
    }

    public String getStopsNo() {
        return stopsNo;
    }

    public void setStopsNo(String stopsNo) {
        this.stopsNo = stopsNo;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getDongCode() {
        return dongCode;
    }

    public void setDongCode(String dongCode) {
        this.dongCode = dongCode;
    }
}
