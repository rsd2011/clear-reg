package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.TemplateAdminService;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.draft.application.response.DraftFormTemplateResponse;

class DraftTemplateAdminControllerExtraTest {

    TemplateAdminService service = mock(TemplateAdminService.class);
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.listGroups(null))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("승인선 템플릿 생성 시 현재 컨텍스트를 전달한다")
    void createApprovalLineTemplateUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest("이름", "BT", "ORG", true, List.of());
        ApprovalLineTemplateResponse response = new ApprovalLineTemplateResponse(
                UUID.randomUUID(),
                "CODE",
                "이름",
                "BT",
                com.example.admin.approval.TemplateScope.ORGANIZATION,
                "ORG",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.createApprovalLineTemplate(eq(request), eq(ctx), anyBoolean())).thenReturn(response);

        ApprovalLineTemplateResponse result = controller.createApprovalLineTemplate(request);

        assertThat(result).isEqualTo(response);
        verify(service).createApprovalLineTemplate(eq(request), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("서식 템플릿 목록 조회 시 파라미터와 컨텍스트를 그대로 전달한다")
    void listDraftFormTemplatesPassesArguments() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        DraftFormTemplateResponse resp = mock(DraftFormTemplateResponse.class);
        when(service.listDraftFormTemplates(eq("BT"), eq("ORG"), eq(false), eq(ctx), eq(true))).thenReturn(List.of(resp));

        List<DraftFormTemplateResponse> result = controller.listDraftFormTemplates("BT", "ORG", false);

        assertThat(result).containsExactly(resp);
    }

    @Test
    @DisplayName("승인 그룹 생성 시 컨텍스트와 true 플래그가 전달된다")
    void createGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalGroupRequest request = new ApprovalGroupRequest("GC", "이름", "설명", "ORG", null);
        ApprovalGroupResponse response = mock(ApprovalGroupResponse.class);
        when(service.createApprovalGroup(eq(request), eq(ctx), eq(true))).thenReturn(response);

        ApprovalGroupResponse result = controller.createGroup(request);

        assertThat(result).isEqualTo(response);
        verify(service).createApprovalGroup(eq(request), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인선 템플릿 목록 조회도 컨텍스트와 파라미터를 그대로 전달한다")
    void listApprovalLineTemplatesPassesArguments() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalLineTemplateResponse response = new ApprovalLineTemplateResponse(
                UUID.randomUUID(),
                "CODE",
                "이름",
                "BT",
                com.example.admin.approval.TemplateScope.ORGANIZATION,
                "ORG",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.listApprovalLineTemplates(eq("BT"), eq("ORG"), eq(false), eq(ctx), eq(true)))
                .thenReturn(List.of(response));

        List<ApprovalLineTemplateResponse> result = controller.listApprovalLineTemplates("BT", "ORG", false);

        assertThat(result).containsExactly(response);
    }
}
