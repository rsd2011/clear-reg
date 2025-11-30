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

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationReferencesTest {

    @Test
    @DisplayName("참조자와 다음 결재자가 수신자에 포함된다")
    void includesReferencesAndNextApprovers() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(TestApprovalHelper.createTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);
        step.delegateTo("delegate", "", OffsetDateTime.now());

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref("ref1", draft)));
        PermissionGroup permGroup = mock(PermissionGroup.class);
        given(permGroup.getCode()).willReturn("PERM_GRP");
        given(permGroup.getApprovalGroupCodes()).willReturn(List.of("GRP"));
        given(permGroupRepo.findByApprovalGroupCode("[\"GRP\"]")).willReturn(List.of(permGroup));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(
                UserAccount.builder().username("next").password("pw").organizationCode("ORG").permissionGroupCode("PERM_GRP").build()
        ));

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
