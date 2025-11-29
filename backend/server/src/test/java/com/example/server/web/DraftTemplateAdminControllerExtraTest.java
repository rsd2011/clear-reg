package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.TemplateAdminService;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.draft.application.dto.DraftFormTemplateResponse;

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

        assertThatThrownBy(() -> controller.listApprovalTemplateRoots(true))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("승인선 템플릿 생성 시 현재 컨텍스트를 전달한다")
    void createApprovalTemplateRootUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                "이름",
                0,
                null,
                true, List.of());
        ApprovalTemplateRootResponse response = new ApprovalTemplateRootResponse(
                UUID.randomUUID(),
                "CODE",
                "이름",
                0,
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.createApprovalTemplateRoot(eq(request), eq(ctx), eq(true))).thenReturn(response);

        ApprovalTemplateRootResponse result = controller.createApprovalTemplateRoot(request);

        assertThat(result).isEqualTo(response);
        verify(service).createApprovalTemplateRoot(eq(request), eq(ctx), eq(true));
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
    @DisplayName("승인선 템플릿 목록 조회도 컨텍스트와 파라미터를 그대로 전달한다")
    void listApprovalTemplateRootsPassesArguments() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalTemplateRootResponse response = new ApprovalTemplateRootResponse(
                UUID.randomUUID(),
                "CODE",
                "이름",
                0,
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.listApprovalTemplateRoots(isNull(), isNull(), eq(false), eq(ctx), eq(true)))
                .thenReturn(List.of(response));

        List<ApprovalTemplateRootResponse> result = controller.listApprovalTemplateRoots(false);

        assertThat(result).containsExactly(response);
    }
}
