package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendResponse {

    private int totalCount;
    private List<NeighborhoodSaleCandidate> candidates = new ArrayList<>();
    private List<LegalDongCandidate> legalDongs = new ArrayList<>();
    private List<NeighborhoodRecommendRequest.LifestylePriority> lifestylePriorities = new ArrayList<>();

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<NeighborhoodSaleCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<NeighborhoodSaleCandidate> candidates) {
        this.candidates = candidates;
    }

    public List<LegalDongCandidate> getLegalDongs() {
        return legalDongs;
    }

    public void setLegalDongs(List<LegalDongCandidate> legalDongs) {
        this.legalDongs = legalDongs;
    }

    public List<NeighborhoodRecommendRequest.LifestylePriority> getLifestylePriorities() {
        return lifestylePriorities;
    }

    public void setLifestylePriorities(List<NeighborhoodRecommendRequest.LifestylePriority> lifestylePriorities) {
        this.lifestylePriorities = lifestylePriorities;
    }

    public static class NeighborhoodSaleCandidate {
        private String id;
        private String aptSeq;
        private String aptName;
        private String lawdCode;
        private String legalDongCode;
        private String legalDongName;
        private String address;
        private Integer amount;
        private Double area;
        private Integer floor;
        private Integer builtYear;
        private String dealDate;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;

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

        public String getAptName() {
            return aptName;
        }

        public void setAptName(String aptName) {
            this.aptName = aptName;
        }

        public String getLawdCode() {
            return lawdCode;
        }

        public void setLawdCode(String lawdCode) {
            this.lawdCode = lawdCode;
        }

        public String getLegalDongCode() {
            return legalDongCode;
        }

        public void setLegalDongCode(String legalDongCode) {
            this.legalDongCode = legalDongCode;
        }

        public String getLegalDongName() {
            return legalDongName;
        }

        public void setLegalDongName(String legalDongName) {
            this.legalDongName = legalDongName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        public Double getArea() {
            return area;
        }

        public void setArea(Double area) {
            this.area = area;
        }

        public Integer getFloor() {
            return floor;
        }

        public void setFloor(Integer floor) {
            this.floor = floor;
        }

        public Integer getBuiltYear() {
            return builtYear;
        }

        public void setBuiltYear(Integer builtYear) {
            this.builtYear = builtYear;
        }

        public String getDealDate() {
            return dealDate;
        }

        public void setDealDate(String dealDate) {
            this.dealDate = dealDate;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }
    }

    public static class LegalDongCandidate {
        private String legalDongCode;
        private String legalDongName;
        private int dealCount;
        private Integer minAmount;
        private Double minDistanceKm;

        public String getLegalDongCode() {
            return legalDongCode;
        }

        public void setLegalDongCode(String legalDongCode) {
            this.legalDongCode = legalDongCode;
        }

        public String getLegalDongName() {
            return legalDongName;
        }

        public void setLegalDongName(String legalDongName) {
            this.legalDongName = legalDongName;
        }

        public int getDealCount() {
            return dealCount;
        }

        public void setDealCount(int dealCount) {
            this.dealCount = dealCount;
        }

        public Integer getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(Integer minAmount) {
            this.minAmount = minAmount;
        }

        public Double getMinDistanceKm() {
            return minDistanceKm;
        }

        public void setMinDistanceKm(Double minDistanceKm) {
            this.minDistanceKm = minDistanceKm;
        }
    }
}
