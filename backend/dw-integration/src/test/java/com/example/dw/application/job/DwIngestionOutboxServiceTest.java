package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

@ExtendWith(MockitoExtension.class)
class DwIngestionOutboxServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    DwIngestionOutboxRepository repository;

    @Mock
    AuditPort auditPort;

    private DwIngestionOutboxService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new DwIngestionOutboxService(repository, FIXED_CLOCK, auditPort);
    }

    @Test
    @DisplayName("아웃박스 잡을 큐에 넣을 때 페이로드가 있으면 함께 저장된다")
    void enqueueSavesPayload() {
        // Given
        DwIngestionJob job = new DwIngestionJob(UUID.randomUUID(), DwIngestionJobType.FETCH_NEXT);
        // When
        service.enqueue(job);
        // Then
        ArgumentCaptor<DwIngestionOutbox> captor = ArgumentCaptor.forClass(DwIngestionOutbox.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJobType()).isEqualTo(job.type());
        assertThat(captor.getValue().getStatus()).isEqualTo(DwIngestionOutboxStatus.PENDING);
        verify(auditPort).record(any(), org.mockito.Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("대기 중 아웃박스를 조회해 SENDING으로 잠그고 반환한다")
    void claimPendingLocksOldestPending() {
        // Given
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.lockOldestPending(DwIngestionOutboxStatus.PENDING,
                OffsetDateTime.now(FIXED_CLOCK), PageRequest.of(0, 2)))
                .thenReturn(List.of(entry));
        // When
        List<DwIngestionOutbox> result = service.claimPending(2);
        // Then
        assertThat(result).hasSize(1);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.SENDING);
        assertThat(entry.getLockedBy()).isEqualTo("dw-outbox-relay");
        verify(auditPort).record(any(), org.mockito.Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("완료 표시하면 상태가 SENT로 저장된다")
    void markCompletedSavesSent() {
        // Given
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        // When
        service.markCompleted(id);
        // Then
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.SENT);
        verify(auditPort, atLeastOnce()).record(any(), org.mockito.Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("재시도 예약 시 상태를 PENDING으로 되돌리고 availableAt을 지연시킨다")
    void scheduleRetryDelaysAvailableAt() {
        // Given
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        // When
        service.scheduleRetry(id, Duration.ofMinutes(5), "temp error");
        // Then
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.PENDING);
        assertThat(entry.getAvailableAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK).plusMinutes(5));
        assertThat(entry.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도 예약 대상이 없으면 저장을 시도하지 않는다")
    void scheduleRetrySkipsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        service.scheduleRetry(id, Duration.ofMinutes(1), "missing");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("DeadLetter로 표시하면 상태가 DEAD_LETTER로 바뀌고 마지막 에러가 저장된다")
    void markDeadLetterUpdatesStatusAndError() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.markDeadLetter(id, "fatal error");

        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.DEAD_LETTER);
        assertThat(entry.getLastError()).isEqualTo("fatal error");
        verify(auditPort, atLeastOnce()).record(any(), org.mockito.Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("DeadLetter 대상이 없으면 저장을 시도하지 않는다")
    void markDeadLetterSkipsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        service.markDeadLetter(id, "not-found");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ID로 실패 처리하면 상태를 FAILED로 바꾸고 오류 메시지를 저장한다")
    void markFailedByIdUpdatesStatus() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.markFailed(id, "error");

        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.FAILED);
        assertThat(entry.getLastError()).isEqualTo("error");
        verify(auditPort, atLeastOnce()).record(any(), org.mockito.Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("enqueue 중 중복 키 예외가 발생하면 로그만 남기고 예외를 전파하지 않는다")
    void enqueueSkipsOnDuplicateKey() {
        DwIngestionJob job = new DwIngestionJob(UUID.randomUUID(), DwIngestionJobType.FETCH_NEXT);
        doThrow(new DataIntegrityViolationException("dup"))
                .when(repository).save(any(DwIngestionOutbox.class));

        service.enqueue(job);

        verify(repository, times(1)).save(any(DwIngestionOutbox.class));
    }

    @Test
    @DisplayName("재시도 횟수가 최대치에 도달하면 DEAD_LETTER로 전환한다")
    void scheduleRetryMovesToDeadLetterWhenExceeded() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "first");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "second");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "third");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "fourth");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "fifth");
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.scheduleRetry(id, Duration.ofMinutes(1), "limit");

        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.DEAD_LETTER);
        verify(repository).save(entry);
    }

    @Test
    @DisplayName("엔티티를 찾지 못하면 업데이트하지 않는다")
    void skipUpdateWhenEntryMissing() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        service.markFailed(UUID.randomUUID(), "missing");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("엔티티로 직접 실패 처리하면 상태가 FAILED로 저장된다")
    void markFailedWithEntitySaves() {
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);

        service.markFailed(entry);

        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.FAILED);
    }

    @Test
    @DisplayName("대기 중 아웃박스가 없으면 빈 리스트를 반환하고 상태를 변경하지 않는다")
    void claimPendingReturnsEmptyWhenNoEntries() {
        when(repository.lockOldestPending(DwIngestionOutboxStatus.PENDING,
                OffsetDateTime.now(FIXED_CLOCK), PageRequest.of(0, 5)))
                .thenReturn(List.of());

        List<DwIngestionOutbox> result = service.claimPending(5);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("완료 처리 시 엔티티가 없으면 저장을 시도하지 않는다")
    void markCompletedSkipsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        service.markCompleted(id);

        verify(repository, never()).save(any());
    }
}
