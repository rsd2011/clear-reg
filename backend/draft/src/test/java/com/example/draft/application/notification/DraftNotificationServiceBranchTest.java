package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceBranchTest {

    @Test
    @DisplayName("참조자와 다음 결재자가 없을 때 생성자만 수신자로 반환한다")
    void returnsCreatorOnlyWhenNoExtras() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.admin.approval.ApprovalTemplateStep(
                null,
                1,
                "GRP",
                null
        ));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        given(permGroupRepo.findByApprovalGroupCode("GRP")).willReturn(List.of());

        svc.notify("ACTION", draft, null, step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactly("creator");
    }
}
