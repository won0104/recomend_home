package com.ssafy.prj.batch.commercial;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 상권 통계 Job의 실행 시점만 결정하는 클래스다.
 *
 * <p>실제 수집 로직은 {@link CommercialMetricsCollectTasklet}에 있으며, 이 클래스는
 * 애플리케이션 시작 직후 실행할지와 매주 정해진 시각에 실행할지만 제어한다. 따라서 API 키나
 * 업종 코드 같은 수집 상세 설정은 이 클래스가 아니라 application.properties와 enum에서 관리한다.</p>
 */
@Component
public class CommercialMetricsBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job commercialMetricsBatchJob;

    @Value("${batch.commercial.enabled:false}")
    private boolean enabled;

    @Value("${batch.commercial.run-on-startup:false}")
    private boolean runOnStartup;

    public CommercialMetricsBatchScheduler(
        JobLauncher jobLauncher,
        @Qualifier("commercialMetricsBatchJob") Job commercialMetricsBatchJob
    ) {
        this.jobLauncher = jobLauncher;
        this.commercialMetricsBatchJob = commercialMetricsBatchJob;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartupWhenEnabled() {
        // 개발자가 최초 통계를 채울 때만 true로 둔다. 매번 서버가 켜질 때 대량 API를 호출하지 않는다.
        if (runOnStartup) {
            runBatch("startup");
        }
    }

    @Scheduled(cron = "${batch.commercial.cron}", zone = "Asia/Seoul")
    public void runWeekly() {
        // cron 기본값은 매주 월요일 오전 4시(Asia/Seoul)다.
        runBatch("scheduler");
    }

    private void runBatch(String trigger) {
        // enabled=false면 스케줄러와 시작 이벤트 모두 조용히 종료한다.
        // 테스트나 로컬 개발에서 외부 API 호출을 막을 때 사용한다.
        if (!enabled) {
            return;
        }
        try {
            // requestedAt을 매 실행마다 넣어 Spring Batch가 이전 실행과 구분하게 한다.
            jobLauncher.run(commercialMetricsBatchJob, new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("trigger", trigger)
                .toJobParameters());
        } catch (Exception exception) {
            throw new IllegalStateException("Commercial metrics batch execution failed", exception);
        }
    }
}
