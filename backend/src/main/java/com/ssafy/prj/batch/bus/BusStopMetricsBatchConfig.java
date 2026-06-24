package com.ssafy.prj.batch.bus;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** 서울 버스 정류소 수집과 동별 집계 저장을 하나의 원자적 Spring Batch Job으로 묶는다. */
@Configuration
public class BusStopMetricsBatchConfig {

    @Bean
    public Step busStopMetricsCollectStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        BusStopMetricsCollectTasklet tasklet
    ) {
        return new StepBuilder("busStopMetricsCollectStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .build();
    }

    @Bean
    public Job busStopMetricsBatchJob(
        JobRepository jobRepository,
        @Qualifier("busStopMetricsCollectStep") Step collectStep
    ) {
        return new JobBuilder("busStopMetricsBatchJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(collectStep)
            .build();
    }
}
