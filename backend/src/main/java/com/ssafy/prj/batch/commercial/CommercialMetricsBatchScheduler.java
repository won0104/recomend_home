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
        if (runOnStartup) {
            runBatch("startup");
        }
    }

    @Scheduled(cron = "${batch.commercial.cron}", zone = "Asia/Seoul")
    public void runWeekly() {
        runBatch("scheduler");
    }

    private void runBatch(String trigger) {
        if (!enabled) {
            return;
        }
        try {
            jobLauncher.run(commercialMetricsBatchJob, new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("trigger", trigger)
                .toJobParameters());
        } catch (Exception exception) {
            throw new IllegalStateException("Commercial metrics batch execution failed", exception);
        }
    }
}
