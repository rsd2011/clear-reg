package com.example.draft.application.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

class NoOpDraftBusinessPolicyTest {

    NoOpDraftBusinessPolicy policy = new NoOpDraftBusinessPolicy();

    @Test
    @DisplayName("NoOp 정책은 생성 검증에서 항상 통과시킨다")
    void assertCreatableDoesNothing() {
        policy.assertCreatable("BF", "ORG", "actor");
    }

    @Test
    @DisplayName("NoOp 정책은 상태 변경 후에도 예외를 던지지 않는다")
    void afterStateChangedDoesNothing() {
        policy.afterStateChanged(UUID.randomUUID(), "BF", DraftStatus.IN_REVIEW, DraftAction.SUBMITTED, "actor");
    }
}
