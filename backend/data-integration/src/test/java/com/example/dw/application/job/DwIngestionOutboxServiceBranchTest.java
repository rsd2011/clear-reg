package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.audit.AuditPort;
import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

class DwIngestionOutboxServiceBranchTest {

    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    DwIngestionOutboxRepository repo = mock(DwIngestionOutboxRepository.class);
    AuditPort auditPort = mock(AuditPort.class);
    DwIngestionOutboxService service = new DwIngestionOutboxService(repo, clock, auditPort);

    @Test
    @DisplayName("enqueue 중 중복 키 예외가 나도 예외를 전파하지 않는다")
    void enqueueDuplicateSwallowed() {
        doThrow(new DataIntegrityViolationException("dup")).when(repo).save(any(DwIngestionOutbox.class));

        assertThatNoException().isThrownBy(() -> service.enqueue(DwIngestionJob.fetchNext()));
        verify(auditPort).record(any(), any());
    }

    @Test
    @DisplayName("enqueue 시 auditPort 예외를 삼킨다")
    void enqueueAuditFailureSwallowed() {
        doThrow(new RuntimeException("audit fail")).when(auditPort).record(any(), any());

        assertThatNoException().isThrownBy(() -> service.enqueue(DwIngestionJob.fetchNext()));
    }

    @Test
    @DisplayName("claimPending이 비어 있어도 audit만 남기고 반환한다")
    void claimPendingEmpty() {
        when(repo.lockOldestPending(any(), any(), any())).thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() -> service.claimPending(5));
        verify(auditPort).record(any(), any());
    }

    @Test
    @DisplayName("claimPending이 결과를 반환하면 SENDING으로 전환한다")
    void claimPendingMovesToSending() {
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        when(repo.lockOldestPending(any(), any(), any())).thenReturn(java.util.List.of(entry));

        service.claimPending(1);

        verify(auditPort).record(any(), any());
        verify(repo).lockOldestPending(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(entry.getStatus().name()).isEqualTo("SENDING");
    }

    @Test
    @DisplayName("scheduleRetry가 MAX_RETRY 도달 시 dead-letter로 전환한다")
    void scheduleRetryMaxReached() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        // retryCount 5 이상 만들기
        for (int i = 0; i < 5; i++) {
            outbox.markRetry(clock, Duration.ZERO, "err");
        }
        when(repo.findById(any())).thenReturn(Optional.of(outbox));

        service.scheduleRetry(UUID.randomUUID(), Duration.ofSeconds(1), "boom");

        ArgumentCaptor<DwIngestionOutbox> captor = ArgumentCaptor.forClass(DwIngestionOutbox.class);
        verify(repo).save(captor.capture());
        verify(auditPort).record(any(), any());
        // 상태가 DEAD_LETTER 로 바뀌었음을 간접 검증
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus().name()).isEqualTo("DEAD_LETTER");
    }

    @Test
    @DisplayName("scheduleRetry가 MAX_RETRY 미만이면 RETRY 상태로 이동한다")
    void scheduleRetryBelowMax() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        outbox.markRetry(clock, Duration.ofSeconds(1), "once"); // retryCount=1
        when(repo.findById(any())).thenReturn(Optional.of(outbox));

        service.scheduleRetry(UUID.randomUUID(), Duration.ofSeconds(2), "again");

        ArgumentCaptor<DwIngestionOutbox> captor = ArgumentCaptor.forClass(DwIngestionOutbox.class);
        verify(repo).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("updateEntry가 대상 엔티티를 찾지 못하면 save를 호출하지 않는다")
    void updateEntryMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());

        service.markCompleted(UUID.randomUUID());

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("audit 중 예외가 발생해도 삼킨다")
    void auditExceptionSwallowed() {
        when(repo.findById(any())).thenReturn(Optional.of(DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock)));
        doThrow(new RuntimeException("audit fail")).when(auditPort).record(any(), any());

        assertThatNoException().isThrownBy(() -> service.markCompleted(UUID.randomUUID()));
    }
}
