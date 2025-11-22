package com.example.dw.application.job;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

class DwIngestionOutboxServiceMarkCompletedTest {

    DwIngestionOutboxRepository repository = Mockito.mock(DwIngestionOutboxRepository.class);
    Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);
    DwIngestionOutboxService service = new DwIngestionOutboxService(repository, clock);

    @Test
    @DisplayName("markCompleted는 상태를 SENT로 변경한다")
    void markCompleted_setsSent() {
        UUID id = UUID.randomUUID();
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, clock);
        given(repository.findById(id)).willReturn(Optional.of(outbox));

        service.markCompleted(id);

        verify(repository).save(Mockito.argThat(saved -> saved.getStatus() == DwIngestionOutboxStatus.SENT));
    }
}
