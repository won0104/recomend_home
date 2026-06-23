package com.ssafy.prj.model.dto;

public class HouseInfo {

    private String aptSeq;
    private String sggCd;
    private String umdCd;
    private String umdNm;
    private String jibun;
    private String roadNmSggCd;
    private String roadNm;
    private String roadNmBonbun;
    private String roadNmBubun;
    private String aptNm;
    private Integer buildYear;
    private String latitude;
    private String longitude;

    // join 으로 채워지는 부가 정보
    private String sidoName;
    private String gugunName;
    private Integer dealCount;
    private Long avgPrice;

    public HouseInfo() {
    }

    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }

    public String getSggCd() { return sggCd; }
    public void setSggCd(String sggCd) { this.sggCd = sggCd; }

    public String getUmdCd() { return umdCd; }
    public void setUmdCd(String umdCd) { this.umdCd = umdCd; }

    public String getUmdNm() { return umdNm; }
    public void setUmdNm(String umdNm) { this.umdNm = umdNm; }

    public String getJibun() { return jibun; }
    public void setJibun(String jibun) { this.jibun = jibun; }

    public String getRoadNmSggCd() { return roadNmSggCd; }
    public void setRoadNmSggCd(String roadNmSggCd) { this.roadNmSggCd = roadNmSggCd; }

    public String getRoadNm() { return roadNm; }
    public void setRoadNm(String roadNm) { this.roadNm = roadNm; }

    public String getRoadNmBonbun() { return roadNmBonbun; }
    public void setRoadNmBonbun(String roadNmBonbun) { this.roadNmBonbun = roadNmBonbun; }

    public String getRoadNmBubun() { return roadNmBubun; }
    public void setRoadNmBubun(String roadNmBubun) { this.roadNmBubun = roadNmBubun; }

    public String getAptNm() { return aptNm; }
    public void setAptNm(String aptNm) { this.aptNm = aptNm; }

    public Integer getBuildYear() { return buildYear; }
    public void setBuildYear(Integer buildYear) { this.buildYear = buildYear; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getSidoName() { return sidoName; }
    public void setSidoName(String sidoName) { this.sidoName = sidoName; }

    public String getGugunName() { return gugunName; }
    public void setGugunName(String gugunName) { this.gugunName = gugunName; }

    public Integer getDealCount() { return dealCount; }
    public void setDealCount(Integer dealCount) { this.dealCount = dealCount; }

    public Long getAvgPrice() { return avgPrice; }
    public void setAvgPrice(Long avgPrice) { this.avgPrice = avgPrice; }
}
