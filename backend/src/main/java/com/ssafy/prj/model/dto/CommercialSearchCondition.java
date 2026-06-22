package com.ssafy.prj.model.dto;

public class CommercialSearchCondition {

    private String sigunguCode;
    private String beopjeongName;
    private String categoryLCode;
    private String categoryMCode;
    private String keyword;
    private Integer limit;

    public String getSigunguCode() { return sigunguCode; }
    public void setSigunguCode(String sigunguCode) { this.sigunguCode = sigunguCode; }

    public String getBeopjeongName() { return beopjeongName; }
    public void setBeopjeongName(String beopjeongName) { this.beopjeongName = beopjeongName; }

    public String getCategoryLCode() { return categoryLCode; }
    public void setCategoryLCode(String categoryLCode) { this.categoryLCode = categoryLCode; }

    public String getCategoryMCode() { return categoryMCode; }
    public void setCategoryMCode(String categoryMCode) { this.categoryMCode = categoryMCode; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
