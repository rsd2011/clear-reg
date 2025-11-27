package com.example.draft.application.notification;

import com.example.draft.TestApprovalHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalTemplateStep;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceDistinctRecipientsTest {

    @Test
    @DisplayName("중복 수신자는 제거되어 한 번만 포함된다")
    void removesDuplicateRecipients() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(TestApprovalHelper.createTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        // 중복: actor=creator, delegatedTo도 creator
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        PermissionGroup permGroup = new PermissionGroup("PERM_GRP", "Test Group");
        permGroup.setApprovalGroupCode("GRP");
        given(permGroupRepo.findByApprovalGroupCode("GRP")).willReturn(List.of(permGroup));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(
                UserAccount.builder().username("creator").password("pw").organizationCode("ORG").permissionGroupCode("PERM_GRP").build()
        ));

        svc.notify("ACTION", draft, "creator", step.getId(), "creator", null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactly("creator");
    }
}
