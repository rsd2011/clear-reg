package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.audit.AuditPort;
import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

@ExtendWith(MockitoExtension.class)
class DwIngestionOutboxServiceRetryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    DwIngestionOutboxRepository repository;
    @Mock
    AuditPort auditPort;

    @Test
    @DisplayName("재시도 한계를 넘기지 않으면 PENDING으로 되돌린다")
    void scheduleRetryKeepsPendingWhenUnderLimit() {
        DwIngestionOutboxService service = new DwIngestionOutboxService(repository, FIXED_CLOCK, auditPort);
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.scheduleRetry(id, Duration.ofMinutes(1), "temp");

        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.PENDING);
        verify(repository).save(entry);
    }

    @Test
    @DisplayName("재시도 한계를 초과하면 DeadLetter로 전환하며 오류 메시지를 보존한다")
    void scheduleRetryMovesToDeadLetterAtLimit() {
        DwIngestionOutboxService service = new DwIngestionOutboxService(repository, FIXED_CLOCK, auditPort);
        UUID id = UUID.randomUUID();
        DwIngestionOutbox entry = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, FIXED_CLOCK);
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "1");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "2");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "3");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "4");
        entry.markRetry(FIXED_CLOCK, Duration.ZERO, "5");
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.scheduleRetry(id, Duration.ofMinutes(1), "last error".repeat(100));

        assertThat(entry.getStatus()).isEqualTo(DwIngestionOutboxStatus.DEAD_LETTER);
        assertThat(entry.getLastError()).hasSize(500);
        verify(repository).save(entry);
    }
}
