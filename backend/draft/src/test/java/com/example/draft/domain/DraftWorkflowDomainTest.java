package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftWorkflowDomainTest {

    private Draft newDraft() {
        return Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
    }

    private DraftApprovalStep step(int order, String group) {
        return DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, order, group, ""));
    }

    @Test
    @DisplayName("두 단계 승인 흐름을 통과하면 최종 상태가 COMPLETED 된다")
    void approveTwoStepsCompletesDraft() {
        Draft draft = newDraft();
        DraftApprovalStep s1 = step(1, "GRP1");
        DraftApprovalStep s2 = step(2, "GRP2");
        draft.addApprovalStep(s1);
        draft.addApprovalStep(s2);

        draft.initializeWorkflow(OffsetDateTime.now());
        draft.submit("creator", OffsetDateTime.now());

        draft.approveStep(s1.getId(), "actor1", "ok1", OffsetDateTime.now());
        draft.approveStep(s2.getId(), "actor2", "ok2", OffsetDateTime.now());

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
        assertThat(s1.getState()).isEqualTo(DraftApprovalState.APPROVED);
        assertThat(s2.getState()).isEqualTo(DraftApprovalState.APPROVED);
    }

    @Test
    @DisplayName("승인 대기 상태에서 reject를 호출하면 상태가 REJECTED 로 전환된다")
    void rejectSetsRejectedStatus() {
        Draft draft = newDraft();
        DraftApprovalStep s1 = step(1, "GRP1");
        draft.addApprovalStep(s1);

        draft.initializeWorkflow(OffsetDateTime.now());
        draft.submit("creator", OffsetDateTime.now());

        draft.rejectStep(s1.getId(), "actor", "no", OffsetDateTime.now());

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.REJECTED);
        assertThat(s1.getState()).isEqualTo(DraftApprovalState.REJECTED);
    }
}
