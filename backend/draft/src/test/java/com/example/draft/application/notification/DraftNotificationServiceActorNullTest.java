package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.approval.domain.ApprovalGroup;
import com.example.approval.domain.ApprovalGroupMember;
import com.example.approval.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceActorNullTest {

    @Test
    @DisplayName("actor가 null이면 생성자와 다음 결재자만 수신자로 포함한다")
    void actorNullUsesCreatorAndApprovers() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.of(ApprovalGroup.create("GRP", "n", null, "ORG", null, OffsetDateTime.now())));
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(
                ApprovalGroupMember.create("next", "ORG", null, OffsetDateTime.now())
        ));

        svc.notify("ACTION", draft, null, step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "next");
    }
}
