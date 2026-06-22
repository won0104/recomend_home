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
import com.ssafy.prj.batch.pdf.EstateMarketPdfService;
import com.ssafy.prj.batch.shared.BatchContextKeys;

@Component
public class EstateMarketPdfTasklet implements Tasklet {

    private final EstateMarketPdfService estateMarketPdfService;
    private final EstateMarketBatchDao estateMarketBatchDao;

    public EstateMarketPdfTasklet(EstateMarketPdfService estateMarketPdfService,
            EstateMarketBatchDao estateMarketBatchDao) {
        this.estateMarketPdfService = estateMarketPdfService;
        this.estateMarketBatchDao = estateMarketBatchDao;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        EstateMarketReport report = (EstateMarketReport) ctx.get(BatchContextKeys.MARKET_REPORT);
        List<EstateMarketItem> items = (List<EstateMarketItem>) ctx.get(BatchContextKeys.MARKET_ITEMS);

        String pdfPath = estateMarketPdfService.createPdf(report, items);
        report.setPdfPath(pdfPath);
        estateMarketBatchDao.updatePdfPath(report);
        ctx.put(BatchContextKeys.MARKET_REPORT, report);

        System.out.println("[EstateBatch-PDF] created: " + pdfPath);
        return RepeatStatus.FINISHED;
    }
}
