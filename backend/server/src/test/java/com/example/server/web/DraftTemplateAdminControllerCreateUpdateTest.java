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
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.TemplateScope;
import java.time.OffsetDateTime;

class DraftTemplateAdminControllerCreateUpdateTest {

    private final TemplateAdminService service = mock(TemplateAdminService.class);
    private final AuthContext context = mock(AuthContext.class);
    private final DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @Test
    @DisplayName("승인선 템플릿 업데이트 시 AuthContext와 감사 플래그를 전달한다")
    void updateApprovalTemplateRootUsesContext() {
        AuthContextHolder.set(context);
        ApprovalTemplateStepRequest step = new ApprovalTemplateStepRequest(1, "ROLE");
        ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                "이름",
                0,
                null,
                true, java.util.List.of(step));
        ApprovalTemplateRootResponse response = new ApprovalTemplateRootResponse(UUID.randomUUID(), "CODE", "이름", 0, null, true, OffsetDateTime.now(), OffsetDateTime.now(), java.util.List.of());
        UUID id = UUID.randomUUID();
        given(service.updateApprovalTemplateRoot(eq(id), eq(request), eq(context), eq(true))).willReturn(response);

        ApprovalTemplateRootResponse result = controller.updateApprovalTemplateRoot(id, request);

        assertThat(result).isEqualTo(response);
        verify(service).updateApprovalTemplateRoot(eq(id), eq(request), eq(context), eq(true));
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
