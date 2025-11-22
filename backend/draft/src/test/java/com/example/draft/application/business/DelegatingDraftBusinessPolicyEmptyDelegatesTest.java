package com.example.draft.application.business;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

class DelegatingDraftBusinessPolicyEmptyDelegatesTest {

    @Test
    @DisplayName("delegate가 없으면 assertCreatable/afterStateChanged는 그대로 통과한다")
    void passesWhenNoDelegates() {
        DelegatingDraftBusinessPolicy policy = new DelegatingDraftBusinessPolicy(Collections.emptyList());

        assertThatCode(() -> policy.assertCreatable("BF", "ORG", "actor")).doesNotThrowAnyException();
        assertThatCode(() -> policy.afterStateChanged(UUID.randomUUID(), "BF", DraftStatus.APPROVED, DraftAction.APPROVED, "actor"))
                .doesNotThrowAnyException();
    }
}
