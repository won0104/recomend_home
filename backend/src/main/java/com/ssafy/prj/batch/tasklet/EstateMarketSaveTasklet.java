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
import com.ssafy.prj.batch.shared.BatchContextKeys;

@Component
public class EstateMarketSaveTasklet implements Tasklet {

    private static final int CHUNK_SIZE = 200;

    private final EstateMarketBatchDao estateMarketBatchDao;

    public EstateMarketSaveTasklet(EstateMarketBatchDao estateMarketBatchDao) {
        this.estateMarketBatchDao = estateMarketBatchDao;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        EstateMarketReport report = (EstateMarketReport) ctx.get(BatchContextKeys.MARKET_REPORT);
        List<EstateMarketItem> items = (List<EstateMarketItem>) ctx.get(BatchContextKeys.MARKET_ITEMS);

        estateMarketBatchDao.createReportTable();
        estateMarketBatchDao.widenReportDealYmdColumn();
        estateMarketBatchDao.createItemTable();
        estateMarketBatchDao.insertReport(report);
        if (items != null && !items.isEmpty()) {
            int batchItemUpdates = 0;
            int houseInfoUpdates = 0;
            int houseDealUpdates = 0;
            for (int start = 0; start < items.size(); start += CHUNK_SIZE) {
                List<EstateMarketItem> chunk = items.subList(start, Math.min(start + CHUNK_SIZE, items.size()));
                batchItemUpdates += estateMarketBatchDao.insertItems(chunk);
                houseInfoUpdates += estateMarketBatchDao.upsertHouseInfos(chunk);
                houseDealUpdates += estateMarketBatchDao.insertNewHouseDeals(chunk);
            }
            System.out.println("[EstateBatch-Save] batch items saved=" + batchItemUpdates);
            System.out.println("[EstateBatch-Save] service tables updated. houseinfos=" + houseInfoUpdates
                    + ", new housedeals=" + houseDealUpdates);
        }

        System.out.println("[EstateBatch-Save] report saved. items=" + (items == null ? 0 : items.size()));
        return RepeatStatus.FINISHED;
    }
}
