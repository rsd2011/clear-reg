package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftStatusTest {

    @Test
    @DisplayName("isTerminal은 APPROVED/REJECTED/CANCELLED/WITHDRAWN만 true를 반환한다")
    void isTerminalBranches() {
        assertThat(DraftStatus.APPROVED.isTerminal()).isTrue();
        assertThat(DraftStatus.REJECTED.isTerminal()).isTrue();
        assertThat(DraftStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(DraftStatus.WITHDRAWN.isTerminal()).isTrue();
        assertThat(DraftStatus.DRAFT.isTerminal()).isFalse();
        assertThat(DraftStatus.IN_REVIEW.isTerminal()).isFalse();
        assertThat(DraftStatus.APPROVED_WITH_DEFER.isTerminal()).isFalse();
    }
}
