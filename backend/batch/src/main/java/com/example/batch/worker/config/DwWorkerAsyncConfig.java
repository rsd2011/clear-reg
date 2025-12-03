package com.example.batch.worker.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DwWorkerAsyncConfig {

    @Bean(name = "dwIngestionJobExecutor")
    @ConditionalOnMissingBean(name = "dwIngestionJobExecutor")
    public Executor dwIngestionJobExecutor(
            @Value("${dw.ingestion.worker.executor.core-pool:2}") int corePoolSize,
            @Value("${dw.ingestion.worker.executor.max-pool:8}") int maxPoolSize,
            @Value("${dw.ingestion.worker.executor.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("dw-ingestion-worker-");
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(executor.getCorePoolSize(), maxPoolSize));
        executor.setQueueCapacity(Math.max(10, queueCapacity));
        executor.initialize();
        return executor;
    }
}
