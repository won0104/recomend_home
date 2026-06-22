package com.ssafy.prj.batch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EstateMarketBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job estateMarketBatchJob;

    @Value("${batch.estate.enabled:true}")
    private boolean enabled;

    public EstateMarketBatchScheduler(JobLauncher jobLauncher,
            @Qualifier("estateMarketBatchJob") Job estateMarketBatchJob) {
        this.jobLauncher = jobLauncher;
        this.estateMarketBatchJob = estateMarketBatchJob;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runBatch("startup");
    }

    @Scheduled(cron = "${batch.estate.cron}", zone = "Asia/Seoul")
    public void runScheduledBatch() {
        runBatch("scheduler");
    }

    private void runBatch(String trigger) {
        if (!enabled) {
            return;
        }

        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addLong("requestedAt", System.currentTimeMillis())
                    .addString("trigger", trigger)
                    .toJobParameters();
            System.out.println("[EstateBatch-Scheduler] estateMarketBatchJob start. trigger=" + trigger);
            jobLauncher.run(estateMarketBatchJob, parameters);
        } catch (Exception e) {
            System.err.println("[EstateBatch-Scheduler] batch execution failed");
            e.printStackTrace();
        }
    }
}
