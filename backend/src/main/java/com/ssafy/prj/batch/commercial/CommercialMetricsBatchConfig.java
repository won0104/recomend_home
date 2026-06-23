package com.ssafy.prj.batch.commercial;

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

@Configuration
public class CommercialMetricsBatchConfig {

    @Bean
    public Step commercialMetricsCollectStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CommercialMetricsCollectTasklet tasklet
    ) {
        return new StepBuilder("commercialMetricsCollectStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .build();
    }

    @Bean
    public Job commercialMetricsBatchJob(
        JobRepository jobRepository,
        @Qualifier("commercialMetricsCollectStep") Step collectStep
    ) {
        return new JobBuilder("commercialMetricsBatchJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(collectStep)
            .build();
    }
}
