package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.admin.draft.service.TemplateAdminService;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.dto.RollbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DraftTemplateAdminControllerVersionApiTest {

    TemplateAdminService service = mock(TemplateAdminService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service, objectMapper);
    AuthContext ctx;

    @BeforeEach
    void setUp() {
        ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private DraftFormTemplateResponse createResponse(UUID id, String name, int version) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DraftFormTemplateResponse(
                id,                                    // id
                "CODE-" + id.toString().substring(0, 4), // templateCode
                name,                                  // name
                WorkType.GENERAL,                      // workType
                "{}",                                  // schemaJson
                version,                               // version
                true,                                  // active
                null,                                  // componentPath
                VersionStatus.PUBLISHED,               // status
                ChangeAction.CREATE,                   // changeAction
                null,                                  // changeReason
                "user",                                // changedBy
                "User",                                // changedByName
                now,                                   // changedAt
                now,                                   // validFrom
                null,                                  // validTo
                now,                                   // createdAt
                now);                                  // updatedAt
    }

    @Nested
    @DisplayName("GET /form-templates/{id}")
    class GetDraftFormTemplate {

        @Test
        @DisplayName("템플릿 ID로 단건 조회를 수행한다")
        void returnsTemplateById() {
            UUID id = UUID.randomUUID();
            DraftFormTemplateResponse response = createResponse(id, "Test", 1);
            when(service.findById(id)).thenReturn(response);

            DraftFormTemplateResponse result = controller.getDraftFormTemplate(id);

            assertThat(result.name()).isEqualTo("Test");
            verify(service).findById(id);
        }
    }

    @Nested
    @DisplayName("DELETE /form-templates/{rootId}")
    class DeleteDraftFormTemplate {

        @Test
        @DisplayName("템플릿 삭제를 수행한다")
        void deletesTemplate() {
            UUID rootId = UUID.randomUUID();

            ResponseEntity<Void> result = controller.deleteDraftFormTemplate(rootId);

            assertThat(result.getStatusCode().value()).isEqualTo(204);
            verify(service).deleteTemplate(rootId, ctx);
        }
    }

    @Nested
    @DisplayName("GET /form-templates/root/{rootId}/versions")
    class GetVersionHistory {

        @Test
        @DisplayName("버전 히스토리를 조회한다")
        void returnsVersionHistory() {
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateResponse v1 = createResponse(UUID.randomUUID(), "V1", 1);
            DraftFormTemplateResponse v2 = createResponse(UUID.randomUUID(), "V2", 2);
            when(service.getVersionHistory(rootId)).thenReturn(List.of(v2, v1));

            List<DraftFormTemplateResponse> result = controller.getVersionHistory(rootId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("V2");
            verify(service).getVersionHistory(rootId);
        }
    }

    @Nested
    @DisplayName("POST /form-templates/{id}/rollback")
    class RollbackToVersion {

        @Test
        @DisplayName("특정 버전으로 롤백을 수행한다")
        void rollbacksToVersion() {
            UUID targetId = UUID.randomUUID();
            RollbackRequest request = new RollbackRequest("롤백 사유", false);
            DraftFormTemplateResponse response = createResponse(targetId, "Rollback", 3);
            when(service.rollbackToVersion(eq(targetId), eq("롤백 사유"), any(), eq(false)))
                    .thenReturn(response);

            DraftFormTemplateResponse result = controller.rollbackToVersion(targetId, request);

            assertThat(result.name()).isEqualTo("Rollback");
            verify(service).rollbackToVersion(eq(targetId), eq("롤백 사유"), any(), eq(false));
        }

        @Test
        @DisplayName("덮어쓰기 옵션을 전달한다")
        void passesOverwriteOption() {
            UUID targetId = UUID.randomUUID();
            RollbackRequest request = new RollbackRequest("사유", true);
            DraftFormTemplateResponse response = createResponse(targetId, "Rollback", 4);
            when(service.rollbackToVersion(eq(targetId), eq("사유"), any(), eq(true)))
                    .thenReturn(response);

            controller.rollbackToVersion(targetId, request);

            verify(service).rollbackToVersion(eq(targetId), eq("사유"), any(), eq(true));
        }
    }

    @Nested
    @DisplayName("GET /form-template-schemas/{workType}")
    class GetDefaultSchema {

        @Test
        @DisplayName("업무 유형별 기본 스키마를 JSON으로 반환한다")
        void returnsSchemaAsJson() {
            String result = controller.getDefaultSchema(WorkType.GENERAL);

            assertThat(result).contains("\"version\":\"1.0\"");
            assertThat(result).contains("\"fields\"");
        }

        @Test
        @DisplayName("모든 업무 유형에 대해 스키마를 반환한다")
        void returnsSchemaForAllWorkTypes() {
            for (WorkType workType : WorkType.values()) {
                String result = controller.getDefaultSchema(workType);
                assertThat(result).isNotNull();
                assertThat(result).contains("\"version\"");
            }
        }
    }
}
