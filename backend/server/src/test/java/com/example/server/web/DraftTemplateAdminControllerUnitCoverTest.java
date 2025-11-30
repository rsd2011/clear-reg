package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.draft.application.TemplateAdminService;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;

class DraftTemplateAdminControllerUnitCoverTest {

    TemplateAdminService service = Mockito.mock(TemplateAdminService.class);
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("listApprovalTemplateRoots는 현재 컨텍스트를 전달해 서비스 결과를 반환한다")
    void listApprovalTemplateRootsReturnsServiceResult() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        ApprovalTemplateRootResponse resp = new ApprovalTemplateRootResponse(
                UUID.randomUUID(),
                "CODE",
                "name",
                0,
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.listApprovalTemplateRoots(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(List.of(resp));

        List<ApprovalTemplateRootResponse> result = controller.listApprovalTemplateRoots(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).templateCode()).isEqualTo("CODE");
    }

    @Test
    @DisplayName("updateApprovalTemplateRoot는 서비스에 위임하여 결과를 반환한다")
    void updateApprovalTemplateRootDelegatesToService() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        UUID id = UUID.randomUUID();
        ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest("이름", 0, null, true, List.of());
        ApprovalTemplateRootResponse resp = new ApprovalTemplateRootResponse(
                id, "CODE", "이름", 0, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), List.of());
        when(service.updateApprovalTemplateRoot(any(), any(), any(), anyBoolean())).thenReturn(resp);

        ApprovalTemplateRootResponse result = controller.updateApprovalTemplateRoot(id, request);

        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("createDraftFormTemplate는 서비스에 위임하여 결과를 반환한다")
    void createDraftFormTemplateDelegatesToService() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        DraftFormTemplateRequest request = new DraftFormTemplateRequest("이름", WorkType.GENERAL, "{}", true, null);
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateResponse resp = new DraftFormTemplateResponse(
                UUID.randomUUID(), "CODE", "이름", WorkType.GENERAL, "{}", 1, true,
                VersionStatus.PUBLISHED, ChangeAction.CREATE, null,
                "user", "User", now, now, null, now, now);
        when(service.createDraftFormTemplate(any(), any(), anyBoolean())).thenReturn(resp);

        DraftFormTemplateResponse result = controller.createDraftFormTemplate(request);

        assertThat(result.templateCode()).isEqualTo("CODE");
    }

    @Test
    @DisplayName("updateDraftFormTemplate는 서비스에 위임하여 결과를 반환한다")
    void updateDraftFormTemplateDelegatesToService() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        UUID id = UUID.randomUUID();
        DraftFormTemplateRequest request = new DraftFormTemplateRequest("이름", WorkType.GENERAL, "{}", true, null);
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateResponse resp = new DraftFormTemplateResponse(
                id, "CODE", "이름", WorkType.GENERAL, "{}", 1, true,
                VersionStatus.PUBLISHED, ChangeAction.UPDATE, null,
                "user", "User", now, now, null, now, now);
        when(service.updateDraftFormTemplate(any(), any(), any(), anyBoolean())).thenReturn(resp);

        DraftFormTemplateResponse result = controller.updateDraftFormTemplate(id, request);

        assertThat(result.id()).isEqualTo(id);
    }
}
