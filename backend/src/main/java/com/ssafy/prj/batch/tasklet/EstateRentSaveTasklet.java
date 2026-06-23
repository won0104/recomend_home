package com.ssafy.prj.batch.tasklet;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.dao.EstateRentBatchDao;
import com.ssafy.prj.batch.domain.EstateRentItem;
import com.ssafy.prj.batch.domain.EstateRentReport;
import com.ssafy.prj.batch.geocoding.HouseCoordinateEnrichmentService;
import com.ssafy.prj.batch.shared.BatchContextKeys;

/** 임시 적재된 전월세 API 응답을 houseinfos·houserents에 반영한다. */
@Component
public class EstateRentSaveTasklet implements Tasklet {

    private static final int CHUNK_SIZE = 200;

    private final EstateRentBatchDao estateRentBatchDao;
    private final HouseCoordinateEnrichmentService houseCoordinateEnrichmentService;

    public EstateRentSaveTasklet(EstateRentBatchDao estateRentBatchDao,
            HouseCoordinateEnrichmentService houseCoordinateEnrichmentService) {
        this.estateRentBatchDao = estateRentBatchDao;
        this.houseCoordinateEnrichmentService = houseCoordinateEnrichmentService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var context = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        String reportId = (String) context.get(BatchContextKeys.RENT_REPORT_ID);
        EstateRentReport report = estateRentBatchDao.findReportById(reportId);
        List<EstateRentItem> items = estateRentBatchDao.findItemsByReportId(reportId);
        if (report == null) {
            throw new IllegalStateException("전월세 배치 리포트를 찾을 수 없습니다. reportId=" + reportId);
        }

        estateRentBatchDao.createHouseInfoTable();
        estateRentBatchDao.createHouseRentsTable();
        int houseInfoUpdates = 0;
        int rentUpdates = 0;
        for (int start = 0; start < items.size(); start += CHUNK_SIZE) {
            List<EstateRentItem> chunk = items.subList(start, Math.min(start + CHUNK_SIZE, items.size()));
            houseInfoUpdates += estateRentBatchDao.upsertHouseInfos(chunk);
            rentUpdates += estateRentBatchDao.insertNewHouseRents(chunk);
        }

        int normalizedDongCodes = estateRentBatchDao.normalizeLegalDongCodesForReport(reportId);
        houseCoordinateEnrichmentService.enrichTargets("EstateRentBatch", reportId,
                estateRentBatchDao.findMissingCoordinateTargetsByReportId(reportId),
                estateRentBatchDao::updateHouseCoordinates);

        System.out.println("[EstateRentBatch-Save] houseinfos=" + houseInfoUpdates
                + ", new houserents=" + rentUpdates
                + ", 법정동 코드 보정=" + normalizedDongCodes
                + ", items=" + items.size());
        return RepeatStatus.FINISHED;
    }
}
