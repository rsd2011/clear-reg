package com.example.draft.application.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;
import com.example.draft.domain.exception.DraftAccessDeniedException;

class DelegatingDraftBusinessPolicyTest {

    BusinessWorkflowPort allow = Mockito.mock(BusinessWorkflowPort.class);
    BusinessWorkflowPort deny = Mockito.mock(BusinessWorkflowPort.class);

    @Test
    @DisplayName("모든 delegate가 허용하지 않으면 DraftAccessDeniedException을 던진다")
    void assertCreatableDenied() {
        when(allow.canCreate("BF", "ORG", "actor")).thenReturn(true);
        when(deny.canCreate("BF", "ORG", "actor")).thenReturn(false);
        DelegatingDraftBusinessPolicy policy = new DelegatingDraftBusinessPolicy(List.of(allow, deny));

        assertThatThrownBy(() -> policy.assertCreatable("BF", "ORG", "actor"))
                .isInstanceOf(DraftAccessDeniedException.class);
    }

    @Test
    @DisplayName("afterStateChanged는 모든 delegate에 onStateChanged를 호출한다")
    void afterStateChangedInvokesAll() {
        when(allow.canCreate("BF", "ORG", "actor")).thenReturn(true);
        when(deny.canCreate("BF", "ORG", "actor")).thenReturn(true);
        DelegatingDraftBusinessPolicy policy = new DelegatingDraftBusinessPolicy(List.of(allow, deny));

        policy.afterStateChanged(UUID.randomUUID(), "BF", DraftStatus.IN_REVIEW, DraftAction.SUBMITTED, "actor");

        verify(allow, times(1)).onStateChanged(Mockito.any(), Mockito.eq("BF"), Mockito.eq(DraftStatus.IN_REVIEW), Mockito.eq(DraftAction.SUBMITTED), Mockito.eq("actor"));
        verify(deny, times(1)).onStateChanged(Mockito.any(), Mockito.eq("BF"), Mockito.eq(DraftStatus.IN_REVIEW), Mockito.eq(DraftAction.SUBMITTED), Mockito.eq("actor"));
    }
}

