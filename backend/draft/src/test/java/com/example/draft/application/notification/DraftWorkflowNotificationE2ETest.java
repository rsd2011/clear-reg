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
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftWorkflowNotificationE2ETest {

    @Test
    @DisplayName("두 단계 승인 흐름 후 notify 시 다음 WAITING 단계 멤버와 참조자가 모두 수신자에 포함된다")
    void notifyIncludesNextWaitingStepAndReferences() {
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

        // step1 완료 시나리오(상태를 직접 세팅)
        step1.delegateTo("actor", "ok", OffsetDateTime.now());
        step1.skip("done", OffsetDateTime.now());

        // 참조자, 다음 결재자 스텁
        com.example.draft.domain.DraftReference ref = com.example.draft.domain.DraftReference.create(
                "ref-user", "ORG", "creator", OffsetDateTime.now());
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref));
        PermissionGroup permGroup2 = new PermissionGroup("PERM_GRP2", "Test Group 2");
        permGroup2.setApprovalGroupCode("GRP2");
        given(permGroupRepo.findByApprovalGroupCode("GRP2")).willReturn(List.of(permGroup2));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP2"))).willReturn(List.of(
                UserAccount.builder().username("next-user").password("pw").organizationCode("ORG").permissionGroupCode("PERM_GRP2").build()
        ));

        UUID stepId = step1.getId();
        svc.notify("ACTION", draft, "actor", stepId, null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor", "ref-user", "next-user");
        assertThat(step1.getState()).isEqualTo(DraftApprovalState.SKIPPED); // 상태 변화는 skip으로 시뮬레이션
    }
}
