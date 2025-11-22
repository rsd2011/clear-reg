package com.example.draft.application.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

class NoOpBusinessWorkflowPortTest {

    NoOpBusinessWorkflowPort port = new NoOpBusinessWorkflowPort();

    @Test
    @DisplayName("NoOp 워크플로 포트는 생성 가능 여부를 항상 true로 반환한다")
    void canCreateAlwaysTrue() {
        assertThat(port.canCreate("BF", "ORG", "actor")).isTrue();
    }

    @Test
    @DisplayName("상태 변경 후콜백도 아무 동작 없이 통과한다")
    void onStateChangedDoesNothing() {
        port.onStateChanged(UUID.randomUUID(), "BF", DraftStatus.IN_REVIEW, DraftAction.APPROVED, "actor");
    }
}
