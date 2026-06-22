package com.ssafy.prj.model.dto;

import java.io.Serializable;

public class FavoriteArea implements Serializable {

    private String key;
    private String userId;
    private String sido;
    private String sigungu;
    private String emd;
    private String lawdCd;
    private String createdAt;

    public FavoriteArea() {
    }

    public FavoriteArea(String key, String userId, String sido, String sigungu, String emd, String lawdCd,
            String createdAt) {
        this.key = key;
        this.userId = userId;
        this.sido = sido;
        this.sigungu = sigungu;
        this.emd = emd;
        this.lawdCd = lawdCd;
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getLawdCd() {
        return lawdCd;
    }

    public void setLawdCd(String lawdCd) {
        this.lawdCd = lawdCd;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
