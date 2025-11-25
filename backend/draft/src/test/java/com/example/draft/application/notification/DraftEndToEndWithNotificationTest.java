package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.DraftApprovalStep;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftEndToEndWithNotificationTest {

    @Test
    @DisplayName("두 단계 승인 완료 후 두 번의 notify에서 각 단계 수신자가 모두 포함된다")
    void twoStepApprovalNotifiesRecipients() {
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

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(
                com.example.draft.domain.DraftReference.create("ref1", "ORG", "creator", OffsetDateTime.now())
        ));

        // step1 notify: next approver는 GRP1 멤버
        given(groupRepo.findByGroupCode("GRP1")).willReturn(Optional.of(ApprovalGroup.create("GRP1", "n", null, "ORG", null, OffsetDateTime.now())));
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(
                ApprovalGroupMember.create("user1", "ORG", null, OffsetDateTime.now())
        ));

        UUID step1Id = step1.getId();
        svc.notify("APPROVE", draft, "actor1", step1Id, null, null, OffsetDateTime.now());
        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor1", "ref1", "user1");

        // step1 완료 처리(가시성 제한으로 상태 값만 검증)
        step1.skip("done", OffsetDateTime.now());
        assertThat(step1.getState()).isEqualTo(DraftApprovalState.SKIPPED);

        // step2 notify: 다음 단계 GRP2 멤버
        given(groupRepo.findByGroupCode("GRP2")).willReturn(Optional.of(ApprovalGroup.create("GRP2", "n", null, "ORG", null, OffsetDateTime.now())));
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(
                ApprovalGroupMember.create("user2", "ORG", null, OffsetDateTime.now())
        ));

        UUID step2Id = step2.getId();
        svc.notify("APPROVE", draft, "actor2", step2Id, null, null, OffsetDateTime.now());
        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor2", "ref1", "user2");
    }
}
