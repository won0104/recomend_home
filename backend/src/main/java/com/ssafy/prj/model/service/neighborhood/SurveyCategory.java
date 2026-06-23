package com.ssafy.prj.model.service.neighborhood;

public enum SurveyCategory {
    SCHOOL("학교", 0),
    CAFE("카페/외식", 1),
    CULTURE("운동/문화", 2),
    SHOPPING("장보기/생활편의", 3),
    PARK("공원/산책", 4),
    MEDICAL("의료", 5),
    PET("반려동물", 6);

    private final String label;
    private final int index;

    SurveyCategory(String label, int index) {
        this.label = label;
        this.index = index;
    }

    public String getLabel() {
        return label;
    }

    public int getIndex() {
        return index;
    }
}
