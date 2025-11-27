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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalLineTemplateAdminService;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.TemplateHistoryResponse;
import com.example.admin.permission.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;

class ApprovalLineTemplateControllerTest {

    ApprovalLineTemplateAdminService service = mock(ApprovalLineTemplateAdminService.class);
    ApprovalLineTemplateController controller = new ApprovalLineTemplateController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private AuthContext setupContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);
        return ctx;
    }

    private ApprovalLineTemplateResponse createResponse(UUID id, String name) {
        return new ApprovalLineTemplateResponse(
                id, "tpl-" + id.toString().substring(0, 8), name, 0, "설명",
                true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
    }

    @Test
    @DisplayName("Given: 인증 컨텍스트가 없을 때 / When: listTemplates 호출 / Then: PermissionDeniedException 발생")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.createTemplate(
                new ApprovalLineTemplateRequest("name", 0, "desc", true,
                        List.of(new ApprovalTemplateStepRequest(1, "G1")))))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Nested
    @DisplayName("listTemplates")
    class ListTemplates {

        @Test
        @DisplayName("Given: 컨텍스트 설정됨 / When: listTemplates 호출 / Then: 서비스에 파라미터 전달 및 결과 반환")
        void listTemplatesReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(service.list(any(), anyBoolean())).thenReturn(List.of(createResponse(id, "템플릿")));

            List<ApprovalLineTemplateResponse> result = controller.listTemplates("키워드", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("템플릿");
            verify(service).list(eq("키워드"), eq(true));
        }
    }

    @Nested
    @DisplayName("getTemplate")
    class GetTemplate {

        @Test
        @DisplayName("Given: 유효한 ID / When: getTemplate 호출 / Then: 템플릿 반환")
        void getTemplateReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(service.getById(id)).thenReturn(createResponse(id, "조회 템플릿"));

            ApprovalLineTemplateResponse result = controller.getTemplate(id);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("조회 템플릿");
            verify(service).getById(id);
        }
    }

    @Nested
    @DisplayName("createTemplate")
    class CreateTemplate {

        @Test
        @DisplayName("Given: 유효한 요청 / When: createTemplate 호출 / Then: 201 Created 및 생성된 템플릿 반환")
        void createTemplateReturns201() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest(
                    "새 템플릿", 1, "설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            when(service.create(eq(request), eq(ctx))).thenReturn(createResponse(id, "새 템플릿"));

            ApprovalLineTemplateResponse result = controller.createTemplate(request);

            assertThat(result.name()).isEqualTo("새 템플릿");
            verify(service).create(eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("updateTemplate")
    class UpdateTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: updateTemplate 호출 / Then: 수정된 템플릿 반환")
        void updateTemplateReturns200() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest(
                    "수정 템플릿", 5, "수정 설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "DEPT_HEAD")));

            when(service.update(eq(id), eq(request), eq(ctx))).thenReturn(createResponse(id, "수정 템플릿"));

            ApprovalLineTemplateResponse result = controller.updateTemplate(id, request);

            assertThat(result.name()).isEqualTo("수정 템플릿");
            verify(service).update(eq(id), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class DeleteTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: deleteTemplate 호출 / Then: 204 No Content")
        void deleteTemplateReturns204() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            controller.deleteTemplate(id);

            verify(service).delete(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("activateTemplate")
    class ActivateTemplate {

        @Test
        @DisplayName("Given: 비활성 템플릿 / When: activateTemplate 호출 / Then: 활성화된 템플릿 반환")
        void activateTemplateReturns200() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            ApprovalLineTemplateResponse response = new ApprovalLineTemplateResponse(
                    id, "tpl-123", "템플릿", 0, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            when(service.activate(eq(id), eq(ctx))).thenReturn(response);

            ApprovalLineTemplateResponse result = controller.activateTemplate(id);

            assertThat(result.active()).isTrue();
            verify(service).activate(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("copyTemplate")
    class CopyTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: copyTemplate 호출 / Then: 201 Created 및 복사된 템플릿 반환")
        void copyTemplateReturns201() {
            AuthContext ctx = setupContext();

            UUID sourceId = UUID.randomUUID();
            UUID copiedId = UUID.randomUUID();
            TemplateCopyRequest request = new TemplateCopyRequest("복사 템플릿", "복사 설명");

            TemplateCopyResponse response = new TemplateCopyResponse(
                    copiedId, "tpl-copy", "복사 템플릿", 0, "복사 설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of(),
                    new TemplateCopyResponse.CopiedFromInfo(sourceId, "tpl-original"));
            when(service.copy(eq(sourceId), eq(request), eq(ctx))).thenReturn(response);

            TemplateCopyResponse result = controller.copyTemplate(sourceId, request);

            assertThat(result.name()).isEqualTo("복사 템플릿");
            assertThat(result.copiedFrom().id()).isEqualTo(sourceId);
            verify(service).copy(eq(sourceId), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("getTemplateHistory")
    class GetTemplateHistory {

        @Test
        @DisplayName("Given: 이력이 있는 템플릿 / When: getTemplateHistory 호출 / Then: 이력 목록 반환")
        void getHistoryReturns200() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            TemplateHistoryResponse h1 = new TemplateHistoryResponse(
                    UUID.randomUUID(), "CREATE", "user", "사용자",
                    OffsetDateTime.now().minusDays(1), null);
            TemplateHistoryResponse h2 = new TemplateHistoryResponse(
                    UUID.randomUUID(), "UPDATE", "user", "사용자",
                    OffsetDateTime.now(), null);

            when(service.getHistory(templateId)).thenReturn(List.of(h2, h1));

            List<TemplateHistoryResponse> result = controller.getTemplateHistory(templateId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).action()).isEqualTo("UPDATE");
            verify(service).getHistory(templateId);
        }
    }

    @Nested
    @DisplayName("updateDisplayOrders")
    class UpdateDisplayOrders {

        @Test
        @DisplayName("Given: 여러 템플릿 순서 변경 요청 / When: updateDisplayOrders 호출 / Then: 업데이트된 목록 반환")
        void updateDisplayOrdersReturns200() {
            AuthContext ctx = setupContext();

            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 10),
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id2, 20)));

            ApprovalLineTemplateResponse r1 = new ApprovalLineTemplateResponse(
                    id1, "tpl-1", "템플릿1", 10, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            ApprovalLineTemplateResponse r2 = new ApprovalLineTemplateResponse(
                    id2, "tpl-2", "템플릿2", 20, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            when(service.updateDisplayOrders(eq(request), eq(ctx))).thenReturn(List.of(r1, r2));

            List<ApprovalLineTemplateResponse> result = controller.updateDisplayOrders(request);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(r -> r.displayOrder() == 10);
            assertThat(result).anyMatch(r -> r.displayOrder() == 20);
            verify(service).updateDisplayOrders(eq(request), eq(ctx));
        }
    }
}
