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
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        String reportId = (String) ctx.get(BatchContextKeys.REPORT_ID);

        // PDF 단계도 같은 reportId로 DB에 보관된 거래 목록을 다시 읽는다.
        // 따라서 앞 단계에서 수집한 전체 목록을 Spring Batch 내부 컨텍스트에 직렬화하지 않는다.
        EstateMarketReport report = estateMarketBatchDao.findReportById(reportId);
        List<EstateMarketItem> items = estateMarketBatchDao.findItemsByReportId(reportId);
        if (report == null) {
            throw new IllegalStateException("PDF 생성 대상 리포트를 찾을 수 없습니다. reportId=" + reportId);
        }

        String pdfPath = estateMarketPdfService.createPdf(report, items);
        report.setPdfPath(pdfPath);
        estateMarketBatchDao.updatePdfPath(report);

        System.out.println("[EstateBatch-PDF] created: " + pdfPath);
        return RepeatStatus.FINISHED;
    }
}
