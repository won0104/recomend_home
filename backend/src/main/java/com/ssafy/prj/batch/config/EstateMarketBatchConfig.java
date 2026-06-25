package com.ssafy.prj.batch.config;

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

import com.ssafy.prj.batch.tasklet.EstateMarketCollectTasklet;
import com.ssafy.prj.batch.tasklet.EstateMarketSaveTasklet;

@Configuration
public class EstateMarketBatchConfig {

    @Bean
    public Step estateMarketCollectStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EstateMarketCollectTasklet tasklet) {
        return new StepBuilder("estateMarketCollectStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step estateMarketSaveStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EstateMarketSaveTasklet tasklet) {
        return new StepBuilder("estateMarketSaveStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job estateMarketBatchJob(JobRepository jobRepository,
            @Qualifier("estateMarketCollectStep") Step collectStep,
            @Qualifier("estateMarketSaveStep") Step saveStep) {
        return new JobBuilder("estateMarketBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(collectStep)
                .next(saveStep)
                .build();
    }
}
