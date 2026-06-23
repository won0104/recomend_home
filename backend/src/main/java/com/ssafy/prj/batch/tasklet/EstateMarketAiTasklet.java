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
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var ctx = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        String reportId = (String) ctx.get(BatchContextKeys.REPORT_ID);

        // 수천 건의 거래 목록은 ExecutionContext 대신 배치 테이블에서 조회한다.
        // AI 단계도 reportId 하나만 전달받으므로 메타 테이블 용량 초과가 발생하지 않는다.
        EstateMarketReport report = estateMarketBatchDao.findReportById(reportId);
        List<EstateMarketItem> items = estateMarketBatchDao.findItemsByReportId(reportId);
        if (report == null) {
            throw new IllegalStateException("AI 요약 대상 리포트를 찾을 수 없습니다. reportId=" + reportId);
        }

        String summary = estateMarketAiService.summarize(report, items);
        report.setAiSummary(summary);
        report.setAiStatus("DONE");
        estateMarketBatchDao.updateAiResult(report);

        System.out.println("[EstateBatch-AI] summary updated. reportId=" + report.getReportId());
        return RepeatStatus.FINISHED;
    }
}
