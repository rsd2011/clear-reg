package com.example.draft.application.notification;

import com.example.draft.TestApprovalHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceCommentTest {

    @Test
    @DisplayName("comment가 있으면 payload에 그대로 포함된다")
    void includesCommentWhenPresent() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountProvider userAccountProvider = mock(UserAccountProvider.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountProvider);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(TestApprovalHelper.createTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());
        PermissionGroup permGroup = mock(PermissionGroup.class);
        given(permGroup.getCode()).willReturn("PERM_GRP");
        given(permGroup.getApprovalGroupCodes()).willReturn(List.of("GRP"));
        given(permGroupRepo.findByApprovalGroupCode("[\"GRP\"]")).willReturn(List.of(permGroup));

        UserAccountInfo user = createMockUserAccountInfo("next", "ORG", "PERM_GRP");
        doReturn(List.of(user)).when(userAccountProvider).findByPermissionGroupCodeIn(List.of("PERM_GRP"));

        svc.notify("ACTION", draft, "actor", step.getId(), null, "메모", OffsetDateTime.now());

        assertThat(publisher.lastPayload.comment()).isEqualTo("메모");
    }

    private UserAccountInfo createMockUserAccountInfo(String username, String orgCode, String permGroupCode) {
        UserAccountInfo user = mock(UserAccountInfo.class);
        given(user.getUsername()).willReturn(username);
        given(user.getOrganizationCode()).willReturn(orgCode);
        org.mockito.Mockito.lenient().when(user.getPermissionGroupCode()).thenReturn(permGroupCode);
        return user;
    }
}
