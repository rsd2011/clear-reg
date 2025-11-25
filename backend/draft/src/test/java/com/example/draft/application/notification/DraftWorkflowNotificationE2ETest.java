package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.approval.domain.ApprovalGroup;
import com.example.approval.domain.ApprovalGroupMember;
import com.example.approval.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftApprovalState;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftWorkflowNotificationE2ETest {

    @Test
    @DisplayName("두 단계 승인 흐름 후 notify 시 다음 WAITING 단계 멤버와 참조자가 모두 수신자에 포함된다")
    void notifyIncludesNextWaitingStepAndReferences() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step1 = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP1", ""));
        DraftApprovalStep step2 = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 2, "GRP2", ""));
        draft.addApprovalStep(step1);
        draft.addApprovalStep(step2);

        // step1 완료 시나리오(상태를 직접 세팅)
        step1.delegateTo("actor", "ok", OffsetDateTime.now());
        step1.skip("done", OffsetDateTime.now());

        // 참조자, 다음 결재자 스텁
        com.example.draft.domain.DraftReference ref = com.example.draft.domain.DraftReference.create(
                "ref-user", "ORG", "creator", OffsetDateTime.now());
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref));
        ApprovalGroup group2 = ApprovalGroup.create("GRP2", "name", null, "ORG", null, OffsetDateTime.now());
        given(groupRepo.findByGroupCode("GRP2")).willReturn(Optional.of(group2));
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(
                ApprovalGroupMember.create("next-user", "ORG", null, OffsetDateTime.now())
        ));

        UUID stepId = step1.getId();
        svc.notify("ACTION", draft, "actor", stepId, null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor", "ref-user", "next-user");
        assertThat(step1.getState()).isEqualTo(DraftApprovalState.SKIPPED); // 상태 변화는 skip으로 시뮬레이션
    }
}
