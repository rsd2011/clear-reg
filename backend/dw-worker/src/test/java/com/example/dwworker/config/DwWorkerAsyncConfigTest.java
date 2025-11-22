package com.example.dwworker.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class DwWorkerAsyncConfigTest {

    private final DwWorkerAsyncConfig config = new DwWorkerAsyncConfig();

    @Test
    @DisplayName("실행자 설정이 최소값을 보장하도록 초기화된다")
    void executorRespectsMinimums() {
        // Given: 음수/작은 설정 값이 주어졌을 때
        int corePool = 0;
        int maxPool = 1;
        int queueCapacity = 5;

        // When: 실행자를 생성하면
        Executor executor = config.dwIngestionJobExecutor(corePool, maxPool, queueCapacity);

        // Then: 최소 코어 1, 큐 10 이상으로 보정되어 초기화된다
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(1);
        assertThat(taskExecutor.getMaxPoolSize()).isGreaterThanOrEqualTo(taskExecutor.getCorePoolSize());
        assertThat(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("정상적인 설정 값이면 그대로 적용된다")
    void executorKeepsProvidedSettings() {
        // Given
        int corePool = 4;
        int maxPool = 8;
        int queueCapacity = 50;

        // When
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.dwIngestionJobExecutor(corePool, maxPool, queueCapacity);

        // Then
        assertThat(executor.getCorePoolSize()).isEqualTo(corePool);
        assertThat(executor.getMaxPoolSize()).isEqualTo(maxPool);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isGreaterThanOrEqualTo(queueCapacity);
    }
}
