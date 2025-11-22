package com.example.draft.application.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftReferenceResponseEqualityTest {

    @Test
    @DisplayName("DraftReferenceResponse equals/hashCode 분기 커버")
    void equalsAndHashCode() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.randomUUID();
        DraftReferenceResponse r1 = new DraftReferenceResponse(id, "user", "ORG", "adder", now);
        DraftReferenceResponse r2 = new DraftReferenceResponse(id, "user", "ORG", "adder", now);
        DraftReferenceResponse rDiff = new DraftReferenceResponse(UUID.randomUUID(), "user", "ORG", "adder", now);

        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(rDiff);
    }
}
