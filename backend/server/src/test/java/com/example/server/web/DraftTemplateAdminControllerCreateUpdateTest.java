package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.draft.application.TemplateAdminService;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.TemplateScope;
import java.time.OffsetDateTime;

class DraftTemplateAdminControllerCreateUpdateTest {

    private final TemplateAdminService service = mock(TemplateAdminService.class);
    private final AuthContext context = mock(AuthContext.class);
    private final DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @Test
    @DisplayName("승인 그룹 생성 시 AuthContext와 감사 플래그를 전달한다")
    void createGroupUsesContext() {
        AuthContextHolder.set(context);
        ApprovalGroupRequest request = new ApprovalGroupRequest("GRP", "이름", null, "ORG", "expr");
        ApprovalGroupResponse response = new ApprovalGroupResponse(UUID.randomUUID(), "GRP", "이름", null, "ORG", "expr", OffsetDateTime.now(), OffsetDateTime.now());
        given(service.createApprovalGroup(eq(request), eq(context), eq(true))).willReturn(response);

        ApprovalGroupResponse result = controller.createGroup(request);

        assertThat(result).isEqualTo(response);
        verify(service).createApprovalGroup(eq(request), eq(context), eq(true));
    }

    @Test
    @DisplayName("승인선 템플릿 업데이트 시 AuthContext와 감사 플래그를 전달한다")
    void updateApprovalLineTemplateUsesContext() {
        AuthContextHolder.set(context);
        ApprovalTemplateStepRequest step = new ApprovalTemplateStepRequest(1, "ROLE", "COND");
        ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest("이름", "BT", "ORG", true, java.util.List.of(step));
        ApprovalLineTemplateResponse response = new ApprovalLineTemplateResponse(UUID.randomUUID(), "CODE", "이름", "BT", com.example.admin.approval.TemplateScope.ORGANIZATION, "ORG", true, OffsetDateTime.now(), OffsetDateTime.now(), java.util.List.of());
        UUID id = UUID.randomUUID();
        given(service.updateApprovalLineTemplate(eq(id), eq(request), eq(context), eq(true))).willReturn(response);

        ApprovalLineTemplateResponse result = controller.updateApprovalLineTemplate(id, request);

        assertThat(result).isEqualTo(response);
        verify(service).updateApprovalLineTemplate(eq(id), eq(request), eq(context), eq(true));
    }

    @Test
    @DisplayName("폼 템플릿 생성 시 AuthContext를 전달한다")
    void createDraftFormTemplateUsesContext() {
        AuthContextHolder.set(context);
        DraftFormTemplateRequest request = new DraftFormTemplateRequest("이름", "BT", "ORG", "{}", true);
        DraftFormTemplateResponse response = new DraftFormTemplateResponse(UUID.randomUUID(), "CODE", "이름", "BT", TemplateScope.ORGANIZATION, "ORG", "{}", 1, true, OffsetDateTime.now(), OffsetDateTime.now());
        given(service.createDraftFormTemplate(eq(request), eq(context), eq(true))).willReturn(response);

        DraftFormTemplateResponse result = controller.createDraftFormTemplate(request);

        assertThat(result).isEqualTo(response);
        verify(service).createDraftFormTemplate(eq(request), eq(context), eq(true));
    }
}
