package com.example.batch.config;

import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.batch.ingestion.DwIngestionService;
import com.example.batch.security.DwBatchAuthContext;
import com.example.auth.permission.context.AuthContextPropagator;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;

/**
 * Spring Batch job wiring for DW ingestion, exposing a single-step tasklet job
 * which defers to the existing ingestion service.
 */
@Configuration
@RequiredArgsConstructor
public class DwBatchConfiguration {

    private final DwIngestionService ingestionService;
    private final DwIngestionOutboxService outboxService;

    @Bean
    public Tasklet dwIngestionTasklet() {
        return (contribution, chunkContext) -> {
            AuthContextPropagator.runWithContext(DwBatchAuthContext.systemContext(),
                    () -> outboxService.enqueue(DwIngestionJob.fetchNext()));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step dwIngestionStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                Tasklet dwIngestionTasklet) {
        return new StepBuilder("dwIngestionStep", jobRepository)
                .tasklet(dwIngestionTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Job dwIngestionJob(JobRepository jobRepository, Step dwIngestionStep) {
        return new JobBuilder("dwIngestionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dwIngestionStep)
                .build();
    }
}
