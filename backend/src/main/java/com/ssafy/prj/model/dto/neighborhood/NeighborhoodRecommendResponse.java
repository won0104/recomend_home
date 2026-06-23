package com.ssafy.prj.model.dto.neighborhood;

import java.util.ArrayList;
import java.util.List;

public class NeighborhoodRecommendResponse {

    private int totalCount;
    private List<String> legalDongCodes = new ArrayList<>();
    private List<String> priorities = new ArrayList<>();

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
}
