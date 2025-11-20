package com.example.dwworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.batch.ingestion.DwIngestionService;
import com.example.batch.ingestion.queue.DwIngestionOutboxRelay;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.dw.application.job.DwIngestionOutboxStatus;
import com.example.dw.domain.DwIngestionOutboxRepository;

@SpringBootTest(properties = {
        "dw.ingestion.queue.max-attempts=2",
        "dw.ingestion.queue.backoff.initial-ms=10",
        "dw.ingestion.queue.backoff.multiplier=1.0",
        "dw.ingestion.queue.backoff.max-ms=10",
        "spring.main.allow-bean-definition-overriding=true"
})
class DwOutboxConcurrencyIntegrationTest {

    @TestConfiguration
    static class SyncExecutorConfiguration {
        @Bean(name = "dwIngestionJobExecutor")
        @Primary
        Executor dwIngestionJobExecutor() {
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("dw-test-executor-");
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(2);
            executor.initialize();
            return executor;
        }
    }

    @MockBean
    private DwIngestionService ingestionService;

    @Autowired
    private DwIngestionOutboxService outboxService;

    @Autowired
    private DwIngestionOutboxRelay relay;

    @Autowired
    private DwIngestionOutboxRepository repository;

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void concurrentRelayProcessesEachOutboxEntryOnce() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(ingestionService.ingestNextFile()).thenAnswer(invocation -> {
            ready.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Optional.empty();
        });

        outboxService.enqueue(DwIngestionJob.fetchNext());
        outboxService.enqueue(DwIngestionJob.fetchNext());

        ExecutorService relayExecutor = Executors.newSingleThreadExecutor();
        Future<?> future = relayExecutor.submit(() -> relay.relay());

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();

        future.get(5, TimeUnit.SECONDS);
        relayExecutor.shutdownNow();

        var stored = awaitOutboxEntries(2);
        assertThat(stored)
                .allSatisfy(entry -> assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.SENT));
        verify(ingestionService, times(2)).ingestNextFile();
    }

    private java.util.List<com.example.dw.domain.DwIngestionOutbox> awaitOutboxEntries(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        java.util.List<com.example.dw.domain.DwIngestionOutbox> entries = java.util.List.of();
        while (System.nanoTime() < deadline) {
            entries = repository.findAll();
            if (entries.size() == expected && entries.stream().allMatch(entry -> entry.getStatus() == DwIngestionOutboxStatus.SENT)) {
                break;
            }
            Thread.sleep(50);
        }
        return entries;
    }
}
