package com.example.draft.application.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftHistoryResponseEqualityTest {

    @Test
    @DisplayName("DraftHistoryResponse equals/hashCode 분기 커버")
    void equalsAndHashCode() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.randomUUID();
        DraftHistoryResponse r1 = new DraftHistoryResponse(id, "EVT", "actor", "details", now);
        DraftHistoryResponse r2 = new DraftHistoryResponse(id, "EVT", "actor", "details", now);
        DraftHistoryResponse rDiff = new DraftHistoryResponse(UUID.randomUUID(), "EVT", "actor", "details", now);

        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(rDiff);
    }
}
