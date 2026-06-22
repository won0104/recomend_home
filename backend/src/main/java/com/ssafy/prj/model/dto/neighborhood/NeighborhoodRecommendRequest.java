package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendRequest {

    private Origin origin;
    private Double radiusKm;
    private Integer maxPrice;
    private String dealType;
    private String housingType;
    private List<LifestylePriority> lifestylePriorities = new ArrayList<>();
    private String lifestyleMood;

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getDealType() {
        return dealType;
    }

    public void setDealType(String dealType) {
        this.dealType = dealType;
    }

    public String getHousingType() {
        return housingType;
    }

    public void setHousingType(String housingType) {
        this.housingType = housingType;
    }

    public List<LifestylePriority> getLifestylePriorities() {
        return lifestylePriorities;
    }

    public void setLifestylePriorities(List<LifestylePriority> lifestylePriorities) {
        this.lifestylePriorities = lifestylePriorities;
    }

    public String getLifestyleMood() {
        return lifestyleMood;
    }

    public void setLifestyleMood(String lifestyleMood) {
        this.lifestyleMood = lifestyleMood;
    }

    public static class Origin {
        private String placeName;
        private String address;
        private String roadAddress;
        private Double latitude;
        private Double longitude;
        private String kakaoPlaceId;

        public String getPlaceName() {
            return placeName;
        }

        public void setPlaceName(String placeName) {
            this.placeName = placeName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRoadAddress() {
            return roadAddress;
        }

        public void setRoadAddress(String roadAddress) {
            this.roadAddress = roadAddress;
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

        public String getKakaoPlaceId() {
            return kakaoPlaceId;
        }

        public void setKakaoPlaceId(String kakaoPlaceId) {
            this.kakaoPlaceId = kakaoPlaceId;
        }
    }

    public static class LifestylePriority {
        private Integer rank;
        private String category;

        public Integer getRank() {
            return rank;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}
