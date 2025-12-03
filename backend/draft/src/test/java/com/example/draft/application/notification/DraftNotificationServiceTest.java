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

import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceTest {

    @Test
    @DisplayName("notify는 생성자, actor, 다음 결재자를 포함한 수신자 목록을 만든다")
    void notifyAddsRecipients() {
        DraftNotificationPublisherStub publisher = new DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        PermissionGroupRepository permGroupRepo = mock(PermissionGroupRepository.class);
        UserAccountProvider userAccountProvider = mock(UserAccountProvider.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, permGroupRepo, userAccountProvider);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        ApprovalTemplateStep templateStep = TestApprovalHelper.createTemplateStep(null, 1, "GRP", "");
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(templateStep);
        draft.addApprovalStep(step);

        PermissionGroup permGroup = mock(PermissionGroup.class);
        given(permGroup.getCode()).willReturn("PERM_GRP");
        given(permGroup.getApprovalGroupCodes()).willReturn(List.of("GRP"));

        UserAccountInfo user = createMockUserAccountInfo("user1", "ORG", "PERM_GRP");

        given(permGroupRepo.findByApprovalGroupCode("[\"GRP\"]")).willReturn(List.of(permGroup));
        doReturn(List.of(user)).when(userAccountProvider).findByPermissionGroupCodeIn(List.of("PERM_GRP"));
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());

        svc.notify("ACTION", draft, "actor", step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).contains("creator", "actor", "user1");
    }

    private UserAccountInfo createMockUserAccountInfo(String username, String orgCode, String permGroupCode) {
        UserAccountInfo user = mock(UserAccountInfo.class);
        given(user.getUsername()).willReturn(username);
        given(user.getOrganizationCode()).willReturn(orgCode);
        org.mockito.Mockito.lenient().when(user.getPermissionGroupCode()).thenReturn(permGroupCode);
        return user;
    }

    static final class DraftNotificationPublisherStub implements DraftNotificationPublisher {
        DraftNotificationPayload lastPayload;
        @Override public void publish(DraftNotificationPayload payload) { this.lastPayload = payload; }
    }
}
