package com.example.dw.application.job;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

class DwIngestionOutboxServiceMarkFailedEntryTest {

    DwIngestionOutboxRepository repository = Mockito.mock(DwIngestionOutboxRepository.class);
    Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);
    DwIngestionOutboxService service = new DwIngestionOutboxService(repository, clock);

    @Test
    @DisplayName("markFailed(entry)는 상태를 FAILED로 저장한다")
    void markFailed_entry_setsFailed() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        given(repository.save(outbox)).willReturn(outbox);

        service.markFailed(outbox);

        verify(repository).save(Mockito.argThat(saved -> saved.getStatus() == DwIngestionOutboxStatus.FAILED));
    }
}
