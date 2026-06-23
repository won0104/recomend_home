package com.ssafy.prj.batch.shared;

public final class BatchContextKeys {

    private BatchContextKeys() {
    }

    public static final String REPORT_ID = "reportId";

    /** 전월세 Job이 매매 Job의 reportId와 섞이지 않도록 별도 키를 사용한다. */
    public static final String RENT_REPORT_ID = "rentReportId";
}
