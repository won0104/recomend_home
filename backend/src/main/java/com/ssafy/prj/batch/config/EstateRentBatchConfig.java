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

import com.ssafy.prj.batch.tasklet.EstateRentCollectTasklet;
import com.ssafy.prj.batch.tasklet.EstateRentSaveTasklet;

/** 전월세는 매매와 별도 Job으로 실행해 실패·재실행 범위를 분리한다. */
@Configuration
public class EstateRentBatchConfig {

    @Bean
    public Step estateRentCollectStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EstateRentCollectTasklet tasklet) {
        return new StepBuilder("estateRentCollectStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step estateRentSaveStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EstateRentSaveTasklet tasklet) {
        return new StepBuilder("estateRentSaveStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job estateRentBatchJob(JobRepository jobRepository,
            @Qualifier("estateRentCollectStep") Step collectStep,
            @Qualifier("estateRentSaveStep") Step saveStep) {
        return new JobBuilder("estateRentBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(collectStep)
                .next(saveStep)
                .build();
    }
}
