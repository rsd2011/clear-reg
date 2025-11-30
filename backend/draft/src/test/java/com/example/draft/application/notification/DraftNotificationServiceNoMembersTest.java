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
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceNoMembersTest {

    @Test
    @DisplayName("승인 그룹은 있지만 구성원이 없으면 delegatedTo만 수신자에 추가된다")
    void delegatedOnlyWhenGroupHasNoMembers() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountRepository userAccountRepo = mock(UserAccountRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(com.example.draft.TestApprovalHelper.createTemplateStep(1, "GRP"));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        PermissionGroup permGroup = mock(PermissionGroup.class);
        given(permGroup.getCode()).willReturn("PERM_GRP");
        given(permGroup.getApprovalGroupCodes()).willReturn(List.of("GRP"));
        given(permGroupRepo.findByApprovalGroupCode("[\"GRP\"]")).willReturn(List.of(permGroup));
        given(userAccountRepo.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of());

        svc.notify("ACTION", draft, null, step.getId(), "delegate", null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "delegate");
    }
}
