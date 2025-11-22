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

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceBranchTest {

    @Test
    @DisplayName("참조자와 다음 결재자가 없을 때 생성자만 수신자로 반환한다")
    void returnsCreatorOnlyWhenNoExtras() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.draft.domain.ApprovalTemplateStep(
                null,
                1,
                "GRP",
                null
        ));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.empty());

        svc.notify("ACTION", draft, null, step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactly("creator");
    }
}
