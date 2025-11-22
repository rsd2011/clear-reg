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

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalGroupMember;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationReferencesTest {

    @Test
    @DisplayName("참조자와 다음 결재자가 수신자에 포함된다")
    void includesReferencesAndNextApprovers() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", null));
        draft.addApprovalStep(step);
        step.delegateTo("delegate", "", OffsetDateTime.now());

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref("ref1", draft)));
        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.of(ApprovalGroup.create("GRP", "n", null, "ORG", null, OffsetDateTime.now())));
        ApprovalGroupMember member = ApprovalGroupMember.create("next", "ORG", null, OffsetDateTime.now());
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(member));

        svc.notify("ACTION", draft, "actor", step.getId(), "delegate", "c", OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).contains("creator", "actor", "delegate", "ref1", "next");
    }

    private com.example.draft.domain.DraftReference ref(String userId, Draft draft) {
        com.example.draft.domain.DraftReference reference = com.example.draft.domain.DraftReference.create(
                userId,
                "ORG",
                "actor",
                OffsetDateTime.now()
        );
        return reference;
    }
}
