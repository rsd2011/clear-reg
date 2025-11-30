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

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.dto.PermissionAssignmentDto;
import com.example.admin.permission.dto.PermissionGroupCompareResponse;
import com.example.admin.permission.dto.PermissionGroupDraftRequest;
import com.example.admin.permission.dto.PermissionGroupHistoryResponse;
import com.example.admin.permission.dto.PermissionGroupRootRequest;
import com.example.admin.permission.dto.PermissionGroupRootResponse;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.service.PermissionGroupRootService;
import com.example.admin.permission.service.PermissionGroupVersioningService;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("PermissionGroupRootController 테스트")
class PermissionGroupRootControllerTest {

    PermissionGroupRootService groupService = mock(PermissionGroupRootService.class);
    PermissionGroupVersioningService versionService = mock(PermissionGroupVersioningService.class);
    PermissionGroupRootController controller = new PermissionGroupRootController(groupService, versionService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private AuthContext setupContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", FeatureCode.RULE_MANAGE, ActionCode.READ, List.of());
        AuthContextHolder.set(ctx);
        return ctx;
    }

    private PermissionGroupRootResponse createRootResponse(UUID id, String groupCode, String name) {
        return new PermissionGroupRootResponse(
                id,
                groupCode,
                name,
                "설명",
                true,
                List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                List.of("APG001"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                1,
                false
        );
    }

    private PermissionGroupHistoryResponse createHistoryResponse(UUID id, UUID groupId, int version,
                                                                   VersionStatus status, ChangeAction action,
                                                                   String name, Integer rollbackFromVersion) {
        return new PermissionGroupHistoryResponse(
                id,
                groupId,
                groupId.toString().substring(0, 8).toUpperCase(),
                version,
                OffsetDateTime.now().minusDays(version),
                status == VersionStatus.PUBLISHED ? null : OffsetDateTime.now(),
                name,
                "설명",
                true,
                List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                List.of("APG001"),
                status,
                action,
                action == ChangeAction.CREATE ? null : "변경 사유",
                "user",
                "사용자",
                OffsetDateTime.now(),
                rollbackFromVersion,
                null
        );
    }

    @Test
    @DisplayName("Given: 인증 컨텍스트가 없을 때 / When: createGroup 호출 / Then: PermissionDeniedException 발생")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                "NEW_GROUP",
                "새 그룹",
                "설명",
                true,
                List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                List.of("APG001")
        );

        assertThatThrownBy(() -> controller.createGroup(request))
                .isInstanceOf(PermissionDeniedException.class);
    }

    // ==========================================================================
    // CRUD API 테스트
    // ==========================================================================

    @Nested
    @DisplayName("listGroups")
    class ListGroups {

        @Test
        @DisplayName("Given: 컨텍스트 설정됨 / When: listGroups 호출 / Then: 서비스에 파라미터 전달 및 결과 반환")
        void listGroupsReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(groupService.list(any(), anyBoolean()))
                    .thenReturn(List.of(createRootResponse(id, "GRP001", "그룹")));

            List<PermissionGroupRootResponse> result = controller.listGroups("키워드", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("그룹");
            verify(groupService).list(eq("키워드"), eq(true));
        }
    }

    @Nested
    @DisplayName("getGroup")
    class GetGroup {

        @Test
        @DisplayName("Given: 유효한 ID / When: getGroup 호출 / Then: 그룹 반환")
        void getGroupReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(groupService.getById(id)).thenReturn(createRootResponse(id, "GRP001", "조회 그룹"));

            PermissionGroupRootResponse result = controller.getGroup(id);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("조회 그룹");
            verify(groupService).getById(id);
        }
    }

    @Nested
    @DisplayName("getByGroupCode")
    class GetByGroupCode {

        @Test
        @DisplayName("Given: 유효한 groupCode / When: getByGroupCode 호출 / Then: 그룹 반환")
        void getByGroupCodeReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            String groupCode = "GRP001";
            when(groupService.getByGroupCode(groupCode)).thenReturn(createRootResponse(id, groupCode, "코드 조회 그룹"));

            PermissionGroupRootResponse result = controller.getByGroupCode(groupCode);

            assertThat(result.groupCode()).isEqualTo(groupCode);
            assertThat(result.name()).isEqualTo("코드 조회 그룹");
            verify(groupService).getByGroupCode(groupCode);
        }
    }

    @Nested
    @DisplayName("createGroup")
    class CreateGroup {

        @Test
        @DisplayName("Given: groupCode 없는 요청 / When: createGroup 호출 / Then: 201 Created 및 생성된 그룹 반환")
        void createGroupWithoutCodeReturns201() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    null,  // groupCode 없음
                    "새 그룹",
                    "설명",
                    true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("APG001")
            );

            when(groupService.create(eq(request), eq(ctx)))
                    .thenReturn(createRootResponse(id, "AUTO_GEN", "새 그룹"));

            PermissionGroupRootResponse result = controller.createGroup(request);

            assertThat(result.name()).isEqualTo("새 그룹");
            verify(groupService).create(eq(request), eq(ctx));
        }

        @Test
        @DisplayName("Given: groupCode 포함 요청 / When: createGroup 호출 / Then: 지정된 코드로 생성")
        void createGroupWithCodeReturns201() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    "NEW_GROUP",
                    "새 그룹",
                    "설명",
                    true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("APG001")
            );

            when(groupService.createWithCode(eq("NEW_GROUP"), eq(request), eq(ctx)))
                    .thenReturn(createRootResponse(id, "NEW_GROUP", "새 그룹"));

            PermissionGroupRootResponse result = controller.createGroup(request);

            assertThat(result.name()).isEqualTo("새 그룹");
            assertThat(result.groupCode()).isEqualTo("NEW_GROUP");
            verify(groupService).createWithCode(eq("NEW_GROUP"), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("deleteGroup")
    class DeleteGroup {

        @Test
        @DisplayName("Given: 존재하는 그룹 / When: deleteGroup 호출 / Then: 204 No Content")
        void deleteGroupReturns204() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            controller.deleteGroup(id);

            verify(groupService).delete(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("activateGroup")
    class ActivateGroup {

        @Test
        @DisplayName("Given: 비활성 그룹 / When: activateGroup 호출 / Then: 활성화된 그룹 반환")
        void activateGroupReturns200() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            when(groupService.activate(eq(id), eq(ctx)))
                    .thenReturn(createRootResponse(id, "GRP001", "활성화 그룹"));

            PermissionGroupRootResponse result = controller.activateGroup(id);

            assertThat(result.active()).isTrue();
            verify(groupService).activate(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("getGroupHistory")
    class GetGroupHistory {

        @Test
        @DisplayName("Given: 이력이 있는 그룹 / When: getGroupHistory 호출 / Then: 버전 이력 목록 반환")
        void getHistoryReturns200() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse v1 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "그룹", null);
            PermissionGroupHistoryResponse v2 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정된 그룹", null);

            when(groupService.getHistory(groupId)).thenReturn(List.of(v2, v1));

            List<PermissionGroupHistoryResponse> result = controller.getGroupHistory(groupId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).changeAction()).isEqualTo(ChangeAction.UPDATE);
            verify(groupService).getHistory(groupId);
        }
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API 테스트
    // ==========================================================================

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Given: 버전 이력이 있는 그룹 / When: getVersionHistory 호출 / Then: 전체 버전 이력 목록 반환")
        void getVersionHistoryReturnsHistory() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse v1 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "그룹", null);
            PermissionGroupHistoryResponse v2 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 그룹", null);

            when(versionService.getVersionHistory(groupId)).thenReturn(List.of(v2, v1));

            List<PermissionGroupHistoryResponse> result = controller.getVersionHistory(groupId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            verify(versionService).getVersionHistory(groupId);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 특정 버전 존재 / When: getVersion 호출 / Then: 해당 버전 상세 반환")
        void getVersionReturnsSpecificVersion() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 그룹", null);

            when(versionService.getVersion(groupId, 2)).thenReturn(response);

            PermissionGroupHistoryResponse result = controller.getVersion(groupId, 2);

            assertThat(result.version()).isEqualTo(2);
            assertThat(result.name()).isEqualTo("수정 그룹");
            verify(versionService).getVersion(groupId, 2);
        }
    }

    @Nested
    @DisplayName("getVersionAsOf")
    class GetVersionAsOf {

        @Test
        @DisplayName("Given: 특정 시점에 유효한 버전 / When: getVersionAsOf 호출 / Then: 해당 시점 버전 반환")
        void getVersionAsOfReturnsPointInTimeVersion() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now().minusDays(5);
            PermissionGroupHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), groupId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "그룹", null);

            when(versionService.getVersionAsOf(groupId, asOf)).thenReturn(response);

            PermissionGroupHistoryResponse result = controller.getVersionAsOf(groupId, asOf);

            assertThat(result.version()).isEqualTo(1);
            verify(versionService).getVersionAsOf(groupId, asOf);
        }
    }

    @Nested
    @DisplayName("compareVersions")
    class CompareVersions {

        @Test
        @DisplayName("Given: 두 버전 존재 / When: compareVersions 호출 / Then: 비교 결과 반환")
        void compareVersionsReturnsComparison() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse v1 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "그룹", null);
            PermissionGroupHistoryResponse v2 = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 그룹", null);

            PermissionGroupCompareResponse compareResponse = new PermissionGroupCompareResponse(
                    v1, v2,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.EXPORT)),
                    List.of(),
                    List.of("APG002"),
                    List.of(),
                    true,  // nameChanged
                    true,  // descriptionChanged
                    false  // activeChanged
            );

            when(versionService.compareVersions(groupId, 1, 2)).thenReturn(compareResponse);

            PermissionGroupCompareResponse result = controller.compareVersions(groupId, 1, 2);

            assertThat(result.nameChanged()).isTrue();
            assertThat(result.version1().version()).isEqualTo(1);
            assertThat(result.version2().version()).isEqualTo(2);
            verify(versionService).compareVersions(groupId, 1, 2);
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("Given: 롤백 가능한 버전 / When: rollbackToVersion 호출 / Then: 새 버전 생성 후 반환")
        void rollbackCreatesNewVersion() {
            AuthContext ctx = setupContext();

            UUID groupId = UUID.randomUUID();
            Integer targetVersion = 1;
            String changeReason = "롤백 사유";
            PermissionGroupHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), groupId, 3,
                    VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, "그룹", 1);

            when(versionService.rollbackToVersion(eq(groupId), eq(targetVersion), eq(changeReason), eq(ctx)))
                    .thenReturn(response);

            PermissionGroupHistoryResponse result = controller.rollbackToVersion(groupId, targetVersion, changeReason);

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(result.rollbackFromVersion()).isEqualTo(1);
            verify(versionService).rollbackToVersion(eq(groupId), eq(targetVersion), eq(changeReason), eq(ctx));
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

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse draft = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 그룹", null);

            when(versionService.getDraft(groupId)).thenReturn(draft);

            PermissionGroupHistoryResponse result = controller.getDraft(groupId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            verify(versionService).getDraft(groupId);
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: hasDraft 호출 / Then: true 반환")
        void hasDraftReturnsTrueWhenExists() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            when(versionService.hasDraft(groupId)).thenReturn(true);

            boolean result = controller.hasDraft(groupId);

            assertThat(result).isTrue();
            verify(versionService).hasDraft(groupId);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalseWhenNotExists() {
            setupContext();

            UUID groupId = UUID.randomUUID();
            when(versionService.hasDraft(groupId)).thenReturn(false);

            boolean result = controller.hasDraft(groupId);

            assertThat(result).isFalse();
            verify(versionService).hasDraft(groupId);
        }
    }

    @Nested
    @DisplayName("saveDraft")
    class SaveDraft {

        @Test
        @DisplayName("Given: 유효한 초안 요청 / When: saveDraft 호출 / Then: 초안 저장 후 반환")
        void saveDraftCreatesDraft() {
            AuthContext ctx = setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupDraftRequest request = new PermissionGroupDraftRequest(
                    "초안 이름",
                    "초안 설명",
                    true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("APG001"),
                    "수정 중"
            );
            PermissionGroupHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 이름", null);

            when(versionService.saveDraft(eq(groupId), eq(request), eq(ctx))).thenReturn(response);

            PermissionGroupHistoryResponse result = controller.saveDraft(groupId, request);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(result.name()).isEqualTo("초안 이름");
            verify(versionService).saveDraft(eq(groupId), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: publishDraft 호출 / Then: 초안을 활성 버전으로 전환")
        void publishDraftActivatesDraft() {
            AuthContext ctx = setupContext();

            UUID groupId = UUID.randomUUID();
            PermissionGroupHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), groupId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "게시된 그룹", null);

            when(versionService.publishDraft(eq(groupId), eq(ctx))).thenReturn(response);

            PermissionGroupHistoryResponse result = controller.publishDraft(groupId);

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            verify(versionService).publishDraft(eq(groupId), eq(ctx));
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: discardDraft 호출 / Then: 초안 삭제 완료")
        void discardDraftRemovesDraft() {
            setupContext();

            UUID groupId = UUID.randomUUID();

            controller.discardDraft(groupId);

            verify(versionService).discardDraft(groupId);
        }
    }

    @Nested
    @DisplayName("listWithDraft")
    class ListWithDraft {

        @Test
        @DisplayName("Given: 초안이 있는 그룹 존재 / When: listWithDraft 호출 / Then: 해당 그룹 목록 반환")
        void listWithDraftReturnsOnlyDrafts() {
            setupContext();

            UUID id = UUID.randomUUID();
            PermissionGroupRootResponse response = new PermissionGroupRootResponse(
                    id,
                    "GRP001",
                    "초안 있는 그룹",
                    "설명",
                    true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("APG001"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    1,
                    true  // hasDraft = true
            );
            when(groupService.listWithDraft()).thenReturn(List.of(response));

            List<PermissionGroupRootResponse> result = controller.listWithDraft();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).hasDraft()).isTrue();
            verify(groupService).listWithDraft();
        }
    }
}
