package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.dto.DraftFormTemplateSummary;
import com.example.admin.draft.dto.RollbackRequest;
import com.example.admin.draft.service.DraftFormTemplateService;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DraftFormTemplateControllerTest {

    DraftFormTemplateService service = mock(DraftFormTemplateService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    DraftFormTemplateController controller = new DraftFormTemplateController(service, objectMapper);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private AuthContext setupContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);
        return ctx;
    }

    private DraftFormTemplateResponse createResponse(UUID id, String code) {
        return new DraftFormTemplateResponse(
                id, code, "이름", WorkType.GENERAL, "{}", 1, true,
                null,
                VersionStatus.PUBLISHED,
                ChangeAction.CREATE, null,
                "user", "User", OffsetDateTime.now(), OffsetDateTime.now(), null,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private DraftFormTemplateSummary createSummary(UUID id, String code) {
        return new DraftFormTemplateSummary(
                id, code, "이름", WorkType.GENERAL, true, 1,
                VersionStatus.PUBLISHED, OffsetDateTime.now(), null);
    }

    @Nested
    @DisplayName("인증 검증")
    class AuthValidation {

        @Test
        @DisplayName("인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
        void throwsWhenContextMissing() {
            AuthContextHolder.clear();

            assertThatThrownBy(() -> controller.list(null, true))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        @DisplayName("create 메서드도 인증 컨텍스트가 없으면 예외를 던진다")
        void createThrowsWhenContextMissing() {
            AuthContextHolder.clear();
            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "템플릿", WorkType.GENERAL, "{}", true, "생성");

            assertThatThrownBy(() -> controller.create(request))
                    .isInstanceOf(PermissionDeniedException.class);
        }
    }

    @Nested
    @DisplayName("목록 조회")
    class ListTests {

        @Test
        @DisplayName("서식 템플릿 목록 조회 시 workType과 activeOnly, 컨텍스트를 전달한다")
        void listDraftFormTemplatesPassesArguments() {
            AuthContext ctx = setupContext();
            DraftFormTemplateResponse resp = createResponse(UUID.randomUUID(), "CODE");
            when(service.listDraftFormTemplates(eq(WorkType.HR_UPDATE), eq(false), eq(ctx), eq(true)))
                    .thenReturn(List.of(resp));

            List<DraftFormTemplateResponse> result = controller.list(WorkType.HR_UPDATE, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).templateCode()).isEqualTo("CODE");
            verify(service).listDraftFormTemplates(eq(WorkType.HR_UPDATE), eq(false), eq(ctx), eq(true));
        }

        @Test
        @DisplayName("workType이 null일 때 전체 목록을 조회한다")
        void listWithNullWorkType() {
            AuthContext ctx = setupContext();
            when(service.listDraftFormTemplates(eq(null), eq(true), eq(ctx), eq(true)))
                    .thenReturn(List.of());

            List<DraftFormTemplateResponse> result = controller.list(null, true);

            assertThat(result).isEmpty();
            verify(service).listDraftFormTemplates(eq(null), eq(true), eq(ctx), eq(true));
        }
    }

    @Nested
    @DisplayName("Summary 목록 조회")
    class SummaryTests {

        @Test
        @DisplayName("workType으로 Summary 목록을 조회한다")
        void listSummaryByWorkType() {
            setupContext();
            DraftFormTemplate template = createMockTemplate();
            when(service.listDraftFormTemplateSummaries(eq(WorkType.FILE_EXPORT)))
                    .thenReturn(List.of(template));

            List<DraftFormTemplateSummary> result = controller.listSummary(WorkType.FILE_EXPORT);

            assertThat(result).hasSize(1);
            verify(service).listDraftFormTemplateSummaries(eq(WorkType.FILE_EXPORT));
        }

        @Test
        @DisplayName("workType이 null일 때 전체 Summary 목록을 조회한다")
        void listSummaryAll() {
            setupContext();
            when(service.listDraftFormTemplateSummaries(eq(null))).thenReturn(List.of());

            List<DraftFormTemplateSummary> result = controller.listSummary(null);

            assertThat(result).isEmpty();
            verify(service).listDraftFormTemplateSummaries(eq(null));
        }

        private DraftFormTemplate createMockTemplate() {
            DraftFormTemplate template = mock(DraftFormTemplate.class);
            when(template.getId()).thenReturn(UUID.randomUUID());
            when(template.getTemplateCode()).thenReturn("TPL-001");
            when(template.getName()).thenReturn("템플릿");
            when(template.getWorkType()).thenReturn(WorkType.GENERAL);
            when(template.isActive()).thenReturn(true);
            when(template.getVersion()).thenReturn(1);
            when(template.getStatus()).thenReturn(VersionStatus.PUBLISHED);
            when(template.getValidFrom()).thenReturn(OffsetDateTime.now());
            when(template.getValidTo()).thenReturn(null);
            return template;
        }
    }

    @Nested
    @DisplayName("단건 조회")
    class GetTests {

        @Test
        @DisplayName("단건 조회 시 ID로 템플릿을 조회한다")
        void getTemplateById() {
            setupContext();
            UUID id = UUID.randomUUID();
            DraftFormTemplateResponse resp = createResponse(id, "CODE");
            when(service.findById(eq(id))).thenReturn(resp);

            DraftFormTemplateResponse result = controller.get(id);

            assertThat(result.id()).isEqualTo(id);
            verify(service).findById(eq(id));
        }
    }

    @Nested
    @DisplayName("생성")
    class CreateTests {

        @Test
        @DisplayName("템플릿 생성 요청을 서비스에 전달한다")
        void createTemplate() {
            AuthContext ctx = setupContext();
            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "신규 템플릿", WorkType.GENERAL, "{\"fields\":[]}", true, "생성");
            DraftFormTemplateResponse resp = createResponse(UUID.randomUUID(), "NEW-TPL");
            when(service.createDraftFormTemplate(eq(request), eq(ctx), eq(true))).thenReturn(resp);

            DraftFormTemplateResponse result = controller.create(request);

            assertThat(result.templateCode()).isEqualTo("NEW-TPL");
            verify(service).createDraftFormTemplate(eq(request), eq(ctx), eq(true));
        }
    }

    @Nested
    @DisplayName("수정")
    class UpdateTests {

        @Test
        @DisplayName("템플릿 수정 요청을 서비스에 전달한다")
        void updateTemplate() {
            AuthContext ctx = setupContext();
            UUID id = UUID.randomUUID();
            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "수정된 템플릿", WorkType.HR_UPDATE, "{}", true, "수정");
            DraftFormTemplateResponse resp = createResponse(id, "UPDATED");
            when(service.updateDraftFormTemplate(eq(id), eq(request), eq(ctx), eq(true)))
                    .thenReturn(resp);

            DraftFormTemplateResponse result = controller.update(id, request);

            assertThat(result.templateCode()).isEqualTo("UPDATED");
            verify(service).updateDraftFormTemplate(eq(id), eq(request), eq(ctx), eq(true));
        }
    }

    @Nested
    @DisplayName("삭제")
    class DeleteTests {

        @Test
        @DisplayName("템플릿 삭제 시 204 No Content를 반환한다")
        void deleteTemplate() {
            AuthContext ctx = setupContext();
            UUID rootId = UUID.randomUUID();

            ResponseEntity<Void> result = controller.delete(rootId);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(service).deleteTemplate(eq(rootId), eq(ctx));
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionTests {

        @Test
        @DisplayName("버전 히스토리 조회")
        void getVersionHistory() {
            setupContext();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateResponse v1 = createResponse(UUID.randomUUID(), "V1");
            DraftFormTemplateResponse v2 = createResponse(UUID.randomUUID(), "V2");
            when(service.getVersionHistory(eq(rootId))).thenReturn(List.of(v1, v2));

            List<DraftFormTemplateResponse> result = controller.getVersionHistory(rootId);

            assertThat(result).hasSize(2);
            verify(service).getVersionHistory(eq(rootId));
        }

        @Test
        @DisplayName("특정 버전으로 롤백")
        void rollbackToVersion() {
            AuthContext ctx = setupContext();
            UUID id = UUID.randomUUID();
            RollbackRequest request = new RollbackRequest("롤백 사유", false);
            DraftFormTemplateResponse resp = createResponse(id, "ROLLED-BACK");
            when(service.rollbackToVersion(eq(id), eq("롤백 사유"), eq(ctx), eq(false)))
                    .thenReturn(resp);

            DraftFormTemplateResponse result = controller.rollbackToVersion(id, request);

            assertThat(result.templateCode()).isEqualTo("ROLLED-BACK");
            verify(service).rollbackToVersion(eq(id), eq("롤백 사유"), eq(ctx), eq(false));
        }
    }

    @Nested
    @DisplayName("스키마")
    class SchemaTests {

        @Test
        @DisplayName("업무 유형별 기본 스키마를 JSON으로 반환한다")
        void getDefaultSchema() {
            setupContext();

            String result = controller.getDefaultSchema(WorkType.FILE_EXPORT);

            assertThat(result).isNotBlank();
            assertThat(result).contains("fields");
        }

        @Test
        @DisplayName("GENERAL 업무 유형 스키마를 반환한다")
        void getGeneralSchema() {
            setupContext();

            String result = controller.getDefaultSchema(WorkType.GENERAL);

            assertThat(result).isNotBlank();
        }
    }
}
