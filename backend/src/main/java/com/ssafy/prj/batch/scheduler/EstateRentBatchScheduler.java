package com.ssafy.prj.batch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 전월세 배치의 시작 시점과 정기 실행만 담당한다. */
@Component
public class EstateRentBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job estateRentBatchJob;

    @Value("${batch.estate-rent.enabled:false}")
    private boolean enabled;

    @Value("${batch.estate-rent.run-on-startup:false}")
    private boolean runOnStartup;

    public EstateRentBatchScheduler(JobLauncher jobLauncher,
            @Qualifier("estateRentBatchJob") Job estateRentBatchJob) {
        this.jobLauncher = jobLauncher;
        this.estateRentBatchJob = estateRentBatchJob;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (runOnStartup) {
            runBatch("startup");
        }
    }

    @Scheduled(cron = "${batch.estate-rent.cron}", zone = "Asia/Seoul")
    public void runScheduledBatch() {
        runBatch("scheduler");
    }

    private void runBatch(String trigger) {
        // 기본값 false다. 설정하기 전에는 서버 재시작만으로 외부 API를 호출하지 않는다.
        if (!enabled) {
            return;
        }
        try {
            jobLauncher.run(estateRentBatchJob, new JobParametersBuilder()
                    .addLong("requestedAt", System.currentTimeMillis())
                    .addString("trigger", trigger)
                    .toJobParameters());
        } catch (Exception exception) {
            System.err.println("[EstateRentBatch-Scheduler] batch execution failed");
            exception.printStackTrace();
        }
    }
}
