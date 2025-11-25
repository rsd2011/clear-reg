package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.approval.domain.ApprovalTemplateStep;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceReferenceOnlyTest {

    @Test
    @DisplayName("참조자만 있고 다음 결재자/actor/delegatedTo가 없으면 참조자와 creator만 포함한다")
    void recipientsIncludeReferencesOnly() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        com.example.draft.domain.DraftReference ref = com.example.draft.domain.DraftReference.create(
                "ref-user",
                "ORG",
                "creator",
                OffsetDateTime.now()
        );
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref));
        given(groupRepo.findByGroupCode("GRP")).willReturn(java.util.Optional.empty());
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of());

        UUID stepId = step.getId();
        svc.notify("ACTION", draft, null, stepId, null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "ref-user");
        assertThat(publisher.lastPayload.stepId()).isEqualTo(stepId);
    }
}
