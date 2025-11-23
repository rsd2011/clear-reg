package com.example.dw.application.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditPort;
import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

class DwIngestionOutboxServiceRetryTest {

    DwIngestionOutboxRepository repository = Mockito.mock(DwIngestionOutboxRepository.class);
    Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);
    AuditPort auditPort = Mockito.mock(AuditPort.class);
    DwIngestionOutboxService service = new DwIngestionOutboxService(repository, clock, auditPort);

    @Test
    @DisplayName("재시도 횟수가 최대일 때 markDeadLetter가 호출된다")
    void scheduleRetry_hitsMax_deadLetter() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        // retryCount를 MAX_RETRY(5) 이상으로 맞춤
        outbox.markRetry(clock, Duration.ofSeconds(1), "err1");
        outbox.markRetry(clock, Duration.ofSeconds(1), "err2");
        outbox.markRetry(clock, Duration.ofSeconds(1), "err3");
        outbox.markRetry(clock, Duration.ofSeconds(1), "err4");
        outbox.markRetry(clock, Duration.ofSeconds(1), "err5");
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(outbox));

        service.scheduleRetry(id, Duration.ofSeconds(1), "boom");

        verify(repository).save(Mockito.argThat(saved -> saved.getStatus() == DwIngestionOutboxStatus.DEAD_LETTER));
    }

    @Test
    @DisplayName("재시도 횟수가 남아 있으면 markRetry가 호출된다")
    void scheduleRetry_underMax_marksRetry() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(outbox));

        service.scheduleRetry(id, Duration.ofSeconds(1), "err");

        verify(repository).save(Mockito.argThat(saved -> saved.getStatus() == DwIngestionOutboxStatus.PENDING
                && saved.getRetryCount() == 1));
    }

    @Test
    @DisplayName("markFailed(UUID)는 상태를 FAILED로 업데이트한다")
    void markFailed_setsFailed() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(outbox));

        service.markFailed(id, "boom");

        verify(repository).save(Mockito.argThat(saved -> saved.getStatus() == DwIngestionOutboxStatus.FAILED));
    }
}
