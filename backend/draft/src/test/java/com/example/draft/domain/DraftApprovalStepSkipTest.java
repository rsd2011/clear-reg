package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepSkipTest {

    @Test
    @DisplayName("대기 상태에서는 skip 호출 시 상태가 SKIPPED로 변경된다")
    void skipChangesStateFromWaiting() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        ApprovalTemplateStep templateStep = ApprovalTemplateStep.create(version, 1, group, false);
        version.addStep(templateStep);
        root.activateNewVersion(version, now);

        DraftApprovalStep step = DraftApprovalStep.fromTemplate(templateStep);
        draft.addApprovalStep(step);

        step.skip("no approver", now);

        assertThat(step.getState()).isEqualTo(DraftApprovalState.SKIPPED);
        assertThat(step.getComment()).isEqualTo("no approver");
        assertThat(step.getActedAt()).isEqualTo(now);
    }
}
