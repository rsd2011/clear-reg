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

import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceCreatorOnlyTest {

    @Test
    @DisplayName("actor/delegatedTo/참조/다음결재자 모두 없으면 수신자는 creator만 된다")
    void recipientsOnlyCreatorWhenNoExtras() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.empty());
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of());

        svc.notify("ACTION", draft, null, step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactly("creator");
    }
}
