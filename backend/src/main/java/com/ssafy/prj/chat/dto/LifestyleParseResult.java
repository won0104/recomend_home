package com.ssafy.prj.chat.dto;

import java.util.ArrayList;
import java.util.List;

public class LifestyleParseResult {
    private List<String> priorities = new ArrayList<>();
    private boolean ambiguous;

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities == null ? new ArrayList<>() : priorities;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }
}
