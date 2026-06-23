package com.ssafy.prj.batch.tasklet;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.dao.EstateMarketBatchDao;
import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;
import com.ssafy.prj.batch.geocoding.HouseCoordinateEnrichmentService;
import com.ssafy.prj.batch.shared.BatchContextKeys;

@Component
public class EstateMarketSaveTasklet implements Tasklet {

    private static final int CHUNK_SIZE = 200;

    private final EstateMarketBatchDao estateMarketBatchDao;
    private final HouseCoordinateEnrichmentService houseCoordinateEnrichmentService;

    public EstateMarketSaveTasklet(EstateMarketBatchDao estateMarketBatchDao,
            HouseCoordinateEnrichmentService houseCoordinateEnrichmentService) {
        this.estateMarketBatchDao = estateMarketBatchDao;
        this.houseCoordinateEnrichmentService = houseCoordinateEnrichmentService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        String reportId = (String) ctx.get(BatchContextKeys.REPORT_ID);

        /*
         * 거래 목록은 ExecutionContext가 아니라 batch_estate_market_items에 저장돼 있다.
         * 이 방식은 데이터가 수만 건으로 늘어나도 Spring Batch 메타 테이블의 용량과 무관하며,
         * 저장 Step만 재시도해야 할 때도 동일한 reportId의 데이터를 다시 사용할 수 있다.
         */
        EstateMarketReport report = estateMarketBatchDao.findReportById(reportId);
        List<EstateMarketItem> items = estateMarketBatchDao.findItemsByReportId(reportId);
        if (report == null) {
            throw new IllegalStateException("매매 배치 리포트를 찾을 수 없습니다. reportId=" + reportId);
        }

        // 기존 스키마 파일을 별도로 실행하지 않은 로컬 DB에서도 매매 배치가 독립적으로 동작하도록 한다.
        estateMarketBatchDao.createHouseInfoTable();
        estateMarketBatchDao.createHouseDealsTable();
        if (items != null && !items.isEmpty()) {
            int houseInfoUpdates = 0;
            int houseDealUpdates = 0;
            for (int start = 0; start < items.size(); start += CHUNK_SIZE) {
                List<EstateMarketItem> chunk = items.subList(start, Math.min(start + CHUNK_SIZE, items.size()));
                houseInfoUpdates += estateMarketBatchDao.upsertHouseInfos(chunk);
                houseDealUpdates += estateMarketBatchDao.insertNewHouseDeals(chunk);
            }
            System.out.println("[EstateBatch-Save] service tables updated. houseinfos=" + houseInfoUpdates
                    + ", new housedeals=" + houseDealUpdates);
        }

        /*
         * 국토부 응답의 법정동명과 dongcodes를 다시 대조해 umd_cd를 보정한다.
         * 이전 적재에서 코드가 00000으로 남으면 추천 SQL과 좌표 대상 SQL이 법정동을 조인하지 못해
         * 대상이 0건이 될 수 있다. 이 보정은 이번 reportId의 아파트만 대상으로 하므로 안전하다.
         */
        int normalizedDongCodes = estateMarketBatchDao.normalizeLegalDongCodesForReport(reportId);
        System.out.println("[EstateBatch-Save] 법정동 코드 보정 건수=" + normalizedDongCodes);

        // 매물·거래 적재가 끝난 뒤에만 좌표 보강을 수행한다. 그래야 좌표 대상 조회가 이번 배치의 최신 아파트를 포함한다.
        houseCoordinateEnrichmentService.enrichForReport(reportId);

        System.out.println("[EstateBatch-Save] report saved. items=" + (items == null ? 0 : items.size()));
        return RepeatStatus.FINISHED;
    }
}
