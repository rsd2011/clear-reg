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

import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.DraftRequest;
import com.example.admin.approval.dto.VersionComparisonResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.approval.service.ApprovalTemplateService;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;

class ApprovalTemplateRootControllerTest {

    ApprovalTemplateRootService service = mock(ApprovalTemplateRootService.class);
    ApprovalTemplateService versionService = mock(ApprovalTemplateService.class);
    ApprovalTemplateRootController controller = new ApprovalTemplateRootController(service, versionService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private AuthContext setupContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
        AuthContextHolder.set(ctx);
        return ctx;
    }

    private ApprovalTemplateRootResponse createResponse(UUID id, String name) {
        return new ApprovalTemplateRootResponse(
                id, "tpl-" + id.toString().substring(0, 8), name, 0, "설명",
                true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
    }

    private VersionHistoryResponse createVersionResponse(UUID id, UUID templateId, int version,
                                                          VersionStatus status, ChangeAction action,
                                                          String name, Integer rollbackFromVersion) {
        return new VersionHistoryResponse(
                id, templateId, version,
                OffsetDateTime.now().minusDays(version), status == VersionStatus.PUBLISHED ? null : OffsetDateTime.now(),
                name, 0, "설명", true,
                status, action,
                action == ChangeAction.CREATE ? null : "변경 사유",
                "user", "사용자", OffsetDateTime.now(),
                rollbackFromVersion, null, List.of());
    }

    @Test
    @DisplayName("Given: 인증 컨텍스트가 없을 때 / When: listTemplates 호출 / Then: PermissionDeniedException 발생")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.createTemplate(
                new ApprovalTemplateRootRequest("name", 0, "desc", true,
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

            List<ApprovalTemplateRootResponse> result = controller.listTemplates("키워드", true);

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

            ApprovalTemplateRootResponse result = controller.getTemplate(id);

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
            ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                    "새 템플릿", 1, "설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            when(service.create(eq(request), eq(ctx))).thenReturn(createResponse(id, "새 템플릿"));

            ApprovalTemplateRootResponse result = controller.createTemplate(request);

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
            ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                    "수정 템플릿", 5, "수정 설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "DEPT_HEAD")));

            when(service.update(eq(id), eq(request), eq(ctx))).thenReturn(createResponse(id, "수정 템플릿"));

            ApprovalTemplateRootResponse result = controller.updateTemplate(id, request);

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
            ApprovalTemplateRootResponse response = new ApprovalTemplateRootResponse(
                    id, "tpl-123", "템플릿", 0, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            when(service.activate(eq(id), eq(ctx))).thenReturn(response);

            ApprovalTemplateRootResponse result = controller.activateTemplate(id);

            assertThat(result.active()).isTrue();
            verify(service).activate(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("getTemplateHistory")
    class GetTemplateHistory {

        @Test
        @DisplayName("Given: 이력이 있는 템플릿 / When: getTemplateHistory 호출 / Then: SCD Type 2 버전 이력 목록 반환")
        void getHistoryReturns200() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse v1 = createVersionResponse(
                    UUID.randomUUID(), templateId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "템플릿", null);
            VersionHistoryResponse v2 = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정된 템플릿", null);

            when(service.getHistory(templateId)).thenReturn(List.of(v2, v1));

            List<VersionHistoryResponse> result = controller.getTemplateHistory(templateId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).changeAction()).isEqualTo(ChangeAction.UPDATE);
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

            ApprovalTemplateRootResponse r1 = new ApprovalTemplateRootResponse(
                    id1, "tpl-1", "템플릿1", 10, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            ApprovalTemplateRootResponse r2 = new ApprovalTemplateRootResponse(
                    id2, "tpl-2", "템플릿2", 20, "설명",
                    true, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
            when(service.updateDisplayOrders(eq(request), eq(ctx))).thenReturn(List.of(r1, r2));

            List<ApprovalTemplateRootResponse> result = controller.updateDisplayOrders(request);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(r -> r.displayOrder() == 10);
            assertThat(result).anyMatch(r -> r.displayOrder() == 20);
            verify(service).updateDisplayOrders(eq(request), eq(ctx));
        }
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API 테스트
    // ==========================================================================

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Given: 버전 이력이 있는 템플릿 / When: getVersionHistory 호출 / Then: 전체 버전 이력 목록 반환")
        void getVersionHistoryReturnsHistory() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse v1 = createVersionResponse(
                    UUID.randomUUID(), templateId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "템플릿", null);
            VersionHistoryResponse v2 = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 템플릿", null);

            when(versionService.getVersionHistory(templateId)).thenReturn(List.of(v2, v1));

            List<VersionHistoryResponse> result = controller.getVersionHistory(templateId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            verify(versionService).getVersionHistory(templateId);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 특정 버전 존재 / When: getVersion 호출 / Then: 해당 버전 상세 반환")
        void getVersionReturnsSpecificVersion() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse response = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 템플릿", null);

            when(versionService.getVersion(templateId, 2)).thenReturn(response);

            VersionHistoryResponse result = controller.getVersion(templateId, 2);

            assertThat(result.version()).isEqualTo(2);
            assertThat(result.name()).isEqualTo("수정 템플릿");
            verify(versionService).getVersion(templateId, 2);
        }
    }

    @Nested
    @DisplayName("getVersionAsOf")
    class GetVersionAsOf {

        @Test
        @DisplayName("Given: 특정 시점에 유효한 버전 / When: getVersionAsOf 호출 / Then: 해당 시점 버전 반환")
        void getVersionAsOfReturnsPointInTimeVersion() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now().minusDays(5);
            VersionHistoryResponse response = createVersionResponse(
                    UUID.randomUUID(), templateId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "템플릿", null);

            when(versionService.getVersionAsOf(templateId, asOf)).thenReturn(response);

            VersionHistoryResponse result = controller.getVersionAsOf(templateId, asOf);

            assertThat(result.version()).isEqualTo(1);
            verify(versionService).getVersionAsOf(templateId, asOf);
        }
    }

    @Nested
    @DisplayName("compareVersions")
    class CompareVersions {

        @Test
        @DisplayName("Given: 두 버전 존재 / When: compareVersions 호출 / Then: 버전 비교 결과 반환")
        void compareVersionsReturnsDiff() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            VersionComparisonResponse.VersionSummary v1Summary = new VersionComparisonResponse.VersionSummary(
                    1, "user1", "사용자1", "2024-01-01T00:00:00Z", "CREATE", null);
            VersionComparisonResponse.VersionSummary v2Summary = new VersionComparisonResponse.VersionSummary(
                    2, "user2", "사용자2", "2024-01-02T00:00:00Z", "UPDATE", "이름 변경");
            List<VersionComparisonResponse.FieldDiff> fieldDiffs = List.of(
                    new VersionComparisonResponse.FieldDiff("name", "이름", "템플릿", "수정 템플릿",
                            VersionComparisonResponse.DiffType.MODIFIED));
            VersionComparisonResponse response = new VersionComparisonResponse(
                    templateId, "tpl-001", v1Summary, v2Summary, fieldDiffs, List.of());

            when(versionService.compareVersions(templateId, 1, 2)).thenReturn(response);

            VersionComparisonResponse result = controller.compareVersions(templateId, 1, 2);

            assertThat(result.version1().version()).isEqualTo(1);
            assertThat(result.version2().version()).isEqualTo(2);
            assertThat(result.fieldDiffs()).hasSize(1);
            verify(versionService).compareVersions(templateId, 1, 2);
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("Given: 롤백 가능한 버전 / When: rollbackToVersion 호출 / Then: 새 버전 생성 후 반환")
        void rollbackCreatesNewVersion() {
            AuthContext ctx = setupContext();

            UUID templateId = UUID.randomUUID();
            Integer targetVersion = 1;
            String changeReason = "롤백 사유";
            VersionHistoryResponse response = createVersionResponse(
                    UUID.randomUUID(), templateId, 3,
                    VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, "템플릿", 1);

            when(versionService.rollbackToVersion(eq(templateId), eq(targetVersion), eq(changeReason), eq(ctx)))
                    .thenReturn(response);

            VersionHistoryResponse result = controller.rollbackToVersion(templateId, targetVersion, changeReason);

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(result.rollbackFromVersion()).isEqualTo(1);
            verify(versionService).rollbackToVersion(eq(templateId), eq(targetVersion), eq(changeReason), eq(ctx));
        }
    }

    // ==========================================================================
    // Draft/Published 시나리오 테스트
    // ==========================================================================

    @Nested
    @DisplayName("getDraft")
    class GetDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: getDraft 호출 / Then: 초안 정보 반환")
        void getDraftReturnsDraft() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse draft = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 템플릿", null);

            when(versionService.getDraft(templateId)).thenReturn(draft);

            VersionHistoryResponse result = controller.getDraft(templateId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            verify(versionService).getDraft(templateId);
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: hasDraft 호출 / Then: true 반환")
        void hasDraftReturnsTrueWhenExists() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            when(versionService.hasDraft(templateId)).thenReturn(true);

            boolean result = controller.hasDraft(templateId);

            assertThat(result).isTrue();
            verify(versionService).hasDraft(templateId);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalseWhenNotExists() {
            setupContext();

            UUID templateId = UUID.randomUUID();
            when(versionService.hasDraft(templateId)).thenReturn(false);

            boolean result = controller.hasDraft(templateId);

            assertThat(result).isFalse();
            verify(versionService).hasDraft(templateId);
        }
    }

    @Nested
    @DisplayName("saveDraft")
    class SaveDraft {

        @Test
        @DisplayName("Given: 유효한 초안 요청 / When: saveDraft 호출 / Then: 초안 저장 후 반환")
        void saveDraftCreatesDraft() {
            AuthContext ctx = setupContext();

            UUID templateId = UUID.randomUUID();
            DraftRequest request = new DraftRequest("초안 이름", 10, "초안 설명", true, "수정 중",
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));
            VersionHistoryResponse response = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 이름", null);

            when(versionService.saveDraft(eq(templateId), eq(request), eq(ctx))).thenReturn(response);

            VersionHistoryResponse result = controller.saveDraft(templateId, request);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(result.name()).isEqualTo("초안 이름");
            verify(versionService).saveDraft(eq(templateId), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: publishDraft 호출 / Then: 초안을 활성 버전으로 전환")
        void publishDraftActivatesDraft() {
            AuthContext ctx = setupContext();

            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse response = createVersionResponse(
                    UUID.randomUUID(), templateId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.PUBLISH, "게시된 템플릿", null);

            when(versionService.publishDraft(eq(templateId), eq(ctx))).thenReturn(response);

            VersionHistoryResponse result = controller.publishDraft(templateId);

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            verify(versionService).publishDraft(eq(templateId), eq(ctx));
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: discardDraft 호출 / Then: 초안 삭제 완료")
        void discardDraftRemovesDraft() {
            setupContext();

            UUID templateId = UUID.randomUUID();

            controller.discardDraft(templateId);

            verify(versionService).discardDraft(templateId);
        }
    }
}
