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

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalGroupMember;
import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceActorEqualsCreatorTest {

    @Test
    @DisplayName("actor가 creator와 같을 때 중복 없이 한 번만 포함된다")
    void actorEqualsCreatorDeduped() {
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
                ApprovalGroupMember.create("creator", "ORG", null, OffsetDateTime.now())
        ));

        svc.notify("ACTION", draft, "creator", step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactly("creator");
    }
}
