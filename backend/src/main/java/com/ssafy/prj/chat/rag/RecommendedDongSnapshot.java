package com.ssafy.prj.chat.rag;

public record RecommendedDongSnapshot(
    int rank,
    String lawdCd,
    String sidoName,
    String sigunguName,
    String dongName
) {

    public String fullName() {
        return String.join(" ", sidoName, sigunguName, dongName).trim();
    }
}
