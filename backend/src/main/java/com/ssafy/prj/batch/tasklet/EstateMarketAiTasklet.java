package com.ssafy.prj.batch.tasklet;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.ai.EstateMarketAiService;
import com.ssafy.prj.batch.dao.EstateMarketBatchDao;
import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;
import com.ssafy.prj.batch.shared.BatchContextKeys;

@Component
public class EstateMarketAiTasklet implements Tasklet {

    private final EstateMarketAiService estateMarketAiService;
    private final EstateMarketBatchDao estateMarketBatchDao;

    public EstateMarketAiTasklet(EstateMarketAiService estateMarketAiService,
            EstateMarketBatchDao estateMarketBatchDao) {
        this.estateMarketAiService = estateMarketAiService;
        this.estateMarketBatchDao = estateMarketBatchDao;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        EstateMarketReport report = (EstateMarketReport) ctx.get(BatchContextKeys.MARKET_REPORT);
        List<EstateMarketItem> items = (List<EstateMarketItem>) ctx.get(BatchContextKeys.MARKET_ITEMS);

        String summary = estateMarketAiService.summarize(report, items);
        report.setAiSummary(summary);
        report.setAiStatus("DONE");
        estateMarketBatchDao.updateAiResult(report);
        ctx.put(BatchContextKeys.MARKET_REPORT, report);

        System.out.println("[EstateBatch-AI] summary updated. reportId=" + report.getReportId());
        return RepeatStatus.FINISHED;
    }
}
