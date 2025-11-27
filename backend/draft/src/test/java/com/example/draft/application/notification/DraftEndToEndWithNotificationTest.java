package com.example.draft.application.notification;

import com.example.draft.TestApprovalHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalTemplateStep;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftEndToEndWithNotificationTest {

    @Test
    @DisplayName("두 단계 승인 완료 후 두 번의 notify에서 각 단계 수신자가 모두 포함된다")
    void twoStepApprovalNotifiesRecipients() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step1 = DraftApprovalStep.fromTemplate(TestApprovalHelper.createTemplateStep(null, 1, "GRP1", ""));
        DraftApprovalStep step2 = DraftApprovalStep.fromTemplate(TestApprovalHelper.createTemplateStep(null, 2, "GRP2", ""));
        draft.addApprovalStep(step1);
        draft.addApprovalStep(step2);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(
                com.example.draft.domain.DraftReference.create("ref1", "ORG", "creator", OffsetDateTime.now())
        ));

        // step1 notify: next approver는 GRP1 멤버
        PermissionGroup permGroup1 = new PermissionGroup("PERM_GRP1", "Test Group 1");
        permGroup1.setApprovalGroupCode("GRP1");
        given(permGroupRepo.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup1));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP1"))).willReturn(List.of(
                UserAccount.builder().username("user1").password("pw").organizationCode("ORG").permissionGroupCode("PERM_GRP1").build()
        ));

        UUID step1Id = step1.getId();
        svc.notify("APPROVE", draft, "actor1", step1Id, null, null, OffsetDateTime.now());
        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor1", "ref1", "user1");

        // step1 완료 처리(가시성 제한으로 상태 값만 검증)
        step1.skip("done", OffsetDateTime.now());
        assertThat(step1.getState()).isEqualTo(DraftApprovalState.SKIPPED);

        // step2 notify: 다음 단계 GRP2 멤버
        PermissionGroup permGroup2 = new PermissionGroup("PERM_GRP2", "Test Group 2");
        permGroup2.setApprovalGroupCode("GRP2");
        given(permGroupRepo.findByApprovalGroupCode("GRP2")).willReturn(List.of(permGroup2));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP2"))).willReturn(List.of(
                UserAccount.builder().username("user2").password("pw").organizationCode("ORG").permissionGroupCode("PERM_GRP2").build()
        ));

        UUID step2Id = step2.getId();
        svc.notify("APPROVE", draft, "actor2", step2Id, null, null, OffsetDateTime.now());
        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor2", "ref1", "user2");
    }
}
