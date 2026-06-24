package com.ssafy.prj.batch.bus;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 서울 버스 정류소 통계 Job의 최초 실행과 정기 실행 시점만 제어한다. */
@Component
public class BusStopMetricsBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job busStopMetricsBatchJob;

    @Value("${batch.bus.enabled:false}")
    private boolean enabled;

    @Value("${batch.bus.run-on-startup:false}")
    private boolean runOnStartup;

    public BusStopMetricsBatchScheduler(
        JobLauncher jobLauncher,
        @Qualifier("busStopMetricsBatchJob") Job busStopMetricsBatchJob
    ) {
        this.jobLauncher = jobLauncher;
        this.busStopMetricsBatchJob = busStopMetricsBatchJob;
    }

    /** 최초 적재 시에만 true로 두며, 평소 서버 재시작마다 12개 API 페이지를 호출하지 않는다. */
    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartupWhenEnabled() {
        if (runOnStartup) {
            runBatch("startup");
        }
    }

    /** 기본값은 매주 일요일 새벽 4시이며, 실제 실행 여부는 enabled 설정으로 제어한다. */
    @Scheduled(cron = "${batch.bus.cron}", zone = "Asia/Seoul")
    public void runWeekly() {
        runBatch("scheduler");
    }

    private void runBatch(String trigger) {
        if (!enabled) {
            return;
        }
        try {
            jobLauncher.run(busStopMetricsBatchJob, new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("trigger", trigger)
                .toJobParameters());
        } catch (Exception exception) {
            throw new IllegalStateException("Bus stop metrics batch execution failed", exception);
        }
    }
}
