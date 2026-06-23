package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendResponse {

    private int totalCount;
    private List<String> legalDongCodes = new ArrayList<>();
    private List<String> priorities = new ArrayList<>();
    /** 프론트 화면에 표시할 최종 상위 3개 추천 결과다. */
    private List<RecommendedNeighborhood> recommendations = new ArrayList<>();

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<String> getLegalDongCodes() {
        return legalDongCodes;
    }

    public void setLegalDongCodes(List<String> legalDongCodes) {
        this.legalDongCodes = legalDongCodes;
    }

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities;
    }

    public List<RecommendedNeighborhood> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendedNeighborhood> recommendations) {
        this.recommendations = recommendations;
    }
}
