package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditPort;
import com.example.dw.domain.DwIngestionOutboxRepository;

class DwIngestionOutboxServiceAuditTest {

    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    DwIngestionOutboxRepository repo = mock(DwIngestionOutboxRepository.class);
    AuditPort auditPort = mock(AuditPort.class);
    DwIngestionOutboxService service = new DwIngestionOutboxService(repo, clock, auditPort);

    @Test
    @DisplayName("audit entryId/extraKey null 조합도 예외 없이 처리한다")
    void auditNullEntryAndExtra() {
        assertThatNoException().isThrownBy(() -> service.enqueue(DwIngestionJob.fetchNext()));
        verify(auditPort).record(any(), any());
    }

    @Test
    @DisplayName("auditPort가 예외를 던져도 claimPending이 실패하지 않는다")
    void auditExceptionClaimPending() {
        doThrow(new RuntimeException("audit fail")).when(auditPort).record(any(), any());

        assertThatNoException().isThrownBy(() -> service.claimPending(1));
        verify(auditPort, times(1)).record(any(), any());
    }
}
