package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwIngestionOutboxEventTest {

    @Test
    @DisplayName("Outbox 이벤트는 ID, jobType, payload를 노출한다")
    void exposesFields() {
        UUID id = UUID.randomUUID();
        DwIngestionOutboxEvent event = new DwIngestionOutboxEvent(id, DwIngestionJobType.FETCH_NEXT, "payload");

        assertThat(event.outboxId()).isEqualTo(id);
        assertThat(event.jobType()).isEqualTo(DwIngestionJobType.FETCH_NEXT);
        assertThat(event.payload()).isEqualTo("payload");
    }
}
