package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftReferenceDeactivateTest {

    @Test
    @DisplayName("DraftReference deactiveate 호출 시 active=false로 전환된다")
    void deactivateTurnsOff() {
        DraftReference ref = DraftReference.create("user", "ORG", "actor", OffsetDateTime.now().minusDays(1));
        ref.deactivate(OffsetDateTime.now());

        assertThat(ref.isActive()).isFalse();
    }
}
