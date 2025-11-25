package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftApprovalState;
import com.example.approval.domain.ApprovalTemplateStep;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceNoWaitingStepTest {

    @Test
    @DisplayName("대기 중인 결재 단계가 없으면 다음 결재자 없이 creator만 포함한다")
    void whenNoWaitingStep_returnsOnlyCreatorAndActor() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);
        step.skip("done", OffsetDateTime.now());

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());

        svc.notify("ACTION", draft, "actor", step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor");
        assertThat(publisher.lastPayload.stepId()).isEqualTo(step.getId());
    }
}
