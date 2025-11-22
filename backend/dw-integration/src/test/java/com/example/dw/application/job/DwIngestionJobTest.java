package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwIngestionJobTest {

    @Test
    @DisplayName("fetchNext는 outboxId가 비어 있고 타입이 FETCH_NEXT인 잡을 반환한다")
    void fetchNextCreatesFetchJob() {
        DwIngestionJob job = DwIngestionJob.fetchNext();

        assertThat(job.outboxId()).isNull();
        assertThat(job.type()).isEqualTo(DwIngestionJobType.FETCH_NEXT);
        assertThat(job.hasOutboxReference()).isFalse();
    }

    @Test
    @DisplayName("outbox 이벤트로부터 잡을 생성하면 참조 ID가 설정된다")
    void fromOutboxSetsReference() {
        UUID outboxId = UUID.randomUUID();
        DwIngestionJob job = DwIngestionJob.fromOutbox(outboxId, DwIngestionJobType.FETCH_NEXT);

        assertThat(job.outboxId()).isEqualTo(outboxId);
        assertThat(job.hasOutboxReference()).isTrue();
    }

    @Test
    @DisplayName("payload는 현재 null이며 추후 확장을 위해 비어 있다")
    void payloadIsNullForNow() {
        DwIngestionJob job = DwIngestionJob.fetchNext();

        assertThat(job.payload()).isNull();
    }
}
