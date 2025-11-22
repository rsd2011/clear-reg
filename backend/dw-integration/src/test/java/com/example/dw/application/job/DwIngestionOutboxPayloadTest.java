package com.example.dw.application.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwIngestionOutboxPayloadTest {

    @Test
    @DisplayName("Outbox 페이로드는 jobType만을 포함한다")
    void containsJobTypeOnly() {
        DwIngestionOutboxPayload payload = new DwIngestionOutboxPayload(DwIngestionJobType.FETCH_NEXT);

        assertThat(payload.jobType()).isEqualTo(DwIngestionJobType.FETCH_NEXT);
    }
}
