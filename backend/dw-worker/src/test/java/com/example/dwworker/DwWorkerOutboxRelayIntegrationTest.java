package com.example.dwworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.audit.AuditPort;
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
class DwWorkerOutboxRelayIntegrationTest {

    @TestConfiguration
    static class SyncExecutorConfiguration {
        @Bean(name = "dwIngestionJobExecutor")
        @Primary
        Executor dwIngestionJobExecutor() {
            return Runnable::run;
        }
    }

    @MockBean
    private DwIngestionService ingestionService;

    @MockBean
    private AuditPort auditPort;

    private final DwIngestionOutboxService outboxService;
    private final DwIngestionOutboxRelay relay;
    private final DwIngestionOutboxRepository repository;

    @Autowired
    DwWorkerOutboxRelayIntegrationTest(DwIngestionOutboxService outboxService,
                                       DwIngestionOutboxRelay relay,
                                       DwIngestionOutboxRepository repository) {
        this.outboxService = outboxService;
        this.relay = relay;
        this.repository = repository;
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void relayDispatchesAndMarksCompleted() {
        when(ingestionService.ingestNextFile()).thenReturn(Optional.empty());

        outboxService.enqueue(DwIngestionJob.fetchNext());
        relay.relay();

        var stored = repository.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getStatus()).isEqualTo(DwIngestionOutboxStatus.SENT);
        assertThat(stored.get(0).getRetryCount()).isZero();
    }

    @Test
    void relaySchedulesRetryWhenWorkerFails() {
        when(ingestionService.ingestNextFile()).thenThrow(new IllegalStateException("boom"));

        outboxService.enqueue(DwIngestionJob.fetchNext());
        relay.relay();

        var stored = repository.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getStatus()).isEqualTo(DwIngestionOutboxStatus.PENDING);
        assertThat(stored.get(0).getRetryCount()).isEqualTo(1);
        assertThat(stored.get(0).getLastError()).contains("boom");
    }
}
