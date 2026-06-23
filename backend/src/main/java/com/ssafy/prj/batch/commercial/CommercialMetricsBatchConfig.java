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

/**
 * 상권 통계 배치를 Spring Batch의 Job과 Step으로 조립한다.
 *
 * <p>Job은 배치 실행 단위이고, Step은 그 안의 실제 작업 단위다. 현재는 API 수집부터 DB 저장까지
 * 하나의 일관된 스냅샷으로 처리해야 하므로 Step 하나로 구성한다. 이 Step에 트랜잭션 관리자를 연결해
 * I2·R1·Q1과 소매·반려 중분류 중 하나라도 실패하면 이번 실행에서 반영한 통계 전체가 롤백되도록 한다.</p>
 */
@Configuration
public class CommercialMetricsBatchConfig {

    @Bean
    public Step commercialMetricsCollectStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CommercialMetricsCollectTasklet tasklet
    ) {
        // Tasklet의 execute() 한 번이 "세 업종 전체 수집 및 저장" 작업을 수행한다.
        return new StepBuilder("commercialMetricsCollectStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .build();
    }

    @Bean
    public Job commercialMetricsBatchJob(
        JobRepository jobRepository,
        @Qualifier("commercialMetricsCollectStep") Step collectStep
    ) {
        // RunIdIncrementer가 실행마다 다른 JobParameter를 만들어 같은 Job도 반복 실행할 수 있게 한다.
        return new JobBuilder("commercialMetricsBatchJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(collectStep)
            .build();
    }
}
