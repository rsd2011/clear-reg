package com.example.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.example.approval.api.ApprovalStatus;

class ApprovalStepTest {

    @Test
    void delegateStoresFields() {
        ApprovalStep step = new ApprovalStep(1, "GRP");
        step.delegateTo("delegatee", "actor", "note", OffsetDateTime.now());

        assertThat(step.getDelegatedTo()).isEqualTo("delegatee");
        assertThat(step.getDelegateComment()).isEqualTo("note");
        assertThat(step.getStatus()).isEqualTo(ApprovalStatus.REQUESTED);
    }

    @Test
    void delegateAfterCompletionThrows() {
        ApprovalStep step = new ApprovalStep(1, "GRP");
        step.approve("actor", OffsetDateTime.now());

        assertThatThrownBy(() -> step.delegateTo("delegatee", "actor", "note", OffsetDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}

