package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.dto.PermissionAssignmentDto;
import com.example.admin.permission.dto.PermissionGroupCompareResponse;
import com.example.admin.permission.dto.PermissionGroupDraftRequest;
import com.example.admin.permission.dto.PermissionGroupHistoryResponse;
import com.example.admin.permission.dto.PermissionGroupRootRequest;
import com.example.admin.permission.exception.PermissionGroupNotFoundException;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PermissionGroupVersioningService 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionGroupVersioningService 테스트")
class PermissionGroupVersioningServiceTest {

    @Mock
    private PermissionGroupRootRepository rootRepository;

    @Mock
    private PermissionGroupRepository versionRepository;

    @Mock
    private PermissionGroupService permissionGroupService;

    private PermissionGroupVersioningService service;

    private AuthContext testContext;

    @BeforeEach
    void setUp() {
        service = new PermissionGroupVersioningService(rootRepository, versionRepository, permissionGroupService);
        testContext = AuthContext.of("testUser", "TEST_ORG", "ADMIN", FeatureCode.RULE_MANAGE, ActionCode.READ);
    }

    private PermissionGroupRoot createTestGroupRoot(String code, String name) {
        OffsetDateTime now = OffsetDateTime.now();
        PermissionGroupRoot root = PermissionGroupRoot.createWithCode(code, now);

        PermissionGroup version = PermissionGroup.create(
                root,
                1,
                name,
                "테스트용 그룹",
                true,
                List.of(new PermissionAssignment(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                List.of("APPROVAL_GRP_1"),
                ChangeAction.CREATE,
                "테스트 생성",
                "SYSTEM",
                "System",
                now);

        root.activateNewVersion(version, now);
        return root;
    }

    @Nested
    @DisplayName("버전 이력 조회")
    class VersionHistoryTest {

        @Test
        @DisplayName("Given 그룹이 존재 When getVersionHistory Then 이력 목록 반환")
        void givenGroupExists_whenGetVersionHistory_thenReturnsHistory() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");
            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findHistoryByRootId(groupId))
                    .willReturn(List.of(root.getCurrentVersion()));

            List<PermissionGroupHistoryResponse> result = service.getVersionHistory(groupId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("Given 그룹이 없음 When getVersionHistory Then 예외")
        void givenGroupNotExists_whenGetVersionHistory_thenThrows() {
            UUID groupId = UUID.randomUUID();
            given(rootRepository.findById(groupId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionHistory(groupId))
                    .isInstanceOf(PermissionGroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("특정 버전 조회")
    class GetVersionTest {

        @Test
        @DisplayName("Given 버전이 존재 When getVersion Then 버전 반환")
        void givenVersionExists_whenGetVersion_thenReturnsVersion() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");
            given(versionRepository.findByRootIdAndVersion(groupId, 1))
                    .willReturn(Optional.of(root.getCurrentVersion()));

            PermissionGroupHistoryResponse result = service.getVersion(groupId, 1);

            assertThat(result.version()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("Given 버전이 없음 When getVersion Then 예외")
        void givenVersionNotExists_whenGetVersion_thenThrows() {
            UUID groupId = UUID.randomUUID();
            given(versionRepository.findByRootIdAndVersion(groupId, 99))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersion(groupId, 99))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("시점 조회")
    class GetVersionAsOfTest {

        @Test
        @DisplayName("Given 해당 시점에 버전 존재 When getVersionAsOf Then 버전 반환")
        void givenVersionExistsAtTime_whenGetVersionAsOf_thenReturnsVersion() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");
            given(versionRepository.findByRootIdAsOf(groupId, asOf))
                    .willReturn(Optional.of(root.getCurrentVersion()));

            PermissionGroupHistoryResponse result = service.getVersionAsOf(groupId, asOf);

            assertThat(result.name()).isEqualTo("Test Group");
        }
    }

    @Nested
    @DisplayName("시점 조회 예외")
    class GetVersionAsOfExceptionTest {

        @Test
        @DisplayName("Given 해당 시점에 버전 없음 When getVersionAsOf Then 예외")
        void givenNoVersionAtTime_whenGetVersionAsOf_thenThrows() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now();
            given(versionRepository.findByRootIdAsOf(groupId, asOf))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionAsOf(groupId, asOf))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("해당 시점에 유효한 버전이 없습니다");
        }
    }

    @Nested
    @DisplayName("버전 비교")
    class CompareVersionsTest {

        @Test
        @DisplayName("Given 두 버전 존재 When compareVersions Then 비교 결과 반환")
        void givenTwoVersionsExist_whenCompareVersions_thenReturnsComparison() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("TEST", now);

            PermissionGroup v1 = PermissionGroup.create(
                    root, 1, "Version 1", "Desc 1", true,
                    List.of(new PermissionAssignment(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("GRP1"),
                    ChangeAction.CREATE, null, "user", "User", now);

            PermissionGroup v2 = PermissionGroup.create(
                    root, 2, "Version 2", "Desc 2", true,
                    List.of(new PermissionAssignment(FeatureCode.AUDIT_LOG, ActionCode.READ),
                            new PermissionAssignment(FeatureCode.AUDIT_LOG, ActionCode.EXPORT)),
                    List.of("GRP1", "GRP2"),
                    ChangeAction.UPDATE, null, "user", "User", now);

            root.activateNewVersion(v2, now);

            given(versionRepository.findVersionsForComparison(groupId, 1, 2))
                    .willReturn(List.of(v1, v2));

            PermissionGroupCompareResponse result = service.compareVersions(groupId, 1, 2);

            assertThat(result.version1().version()).isEqualTo(1);
            assertThat(result.version2().version()).isEqualTo(2);
            assertThat(result.addedAssignments()).hasSize(1);
            assertThat(result.addedApprovalGroups()).containsExactly("GRP2");
            assertThat(result.nameChanged()).isTrue();
        }

        @Test
        @DisplayName("Given 버전 없음 When compareVersions Then 예외")
        void givenVersionsNotFound_whenCompareVersions_thenThrows() {
            UUID groupId = UUID.randomUUID();
            given(versionRepository.findVersionsForComparison(groupId, 1, 2))
                    .willReturn(List.of());

            assertThatThrownBy(() -> service.compareVersions(groupId, 1, 2))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("비교할 버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("롤백")
    class RollbackTest {

        @Test
        @DisplayName("Given 롤백 대상 버전 존재 When rollbackToVersion Then 롤백 버전 생성")
        void givenTargetVersionExists_whenRollback_thenCreatesRollbackVersion() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");
            PermissionGroup currentVersion = root.getCurrentVersion();

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findByRootIdAndVersion(groupId, 1))
                    .willReturn(Optional.of(currentVersion));
            given(versionRepository.findMaxVersionByRootId(groupId)).willReturn(1);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroupHistoryResponse result = service.rollbackToVersion(
                    groupId, 1, "롤백 사유", testContext);

            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(result.rollbackFromVersion()).isEqualTo(1);
            verify(permissionGroupService).evict(root.getGroupCode());
        }

        @Test
        @DisplayName("Given 롤백 대상 버전 없음 When rollbackToVersion Then 예외")
        void givenTargetVersionNotExists_whenRollback_thenThrows() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findByRootIdAndVersion(groupId, 99))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rollbackToVersion(groupId, 99, "롤백 사유", testContext))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("롤백할 버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("초안 관리")
    class DraftManagementTest {

        @Test
        @DisplayName("Given 초안이 없음 When saveDraft Then 새 초안 생성")
        void givenNoDraft_whenSaveDraft_thenCreatesNewDraft() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.empty());
            given(versionRepository.findMaxVersionByRootId(groupId)).willReturn(1);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupDraftRequest request = new PermissionGroupDraftRequest(
                    "Draft Name",
                    "Draft Description",
                    true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("GRP1"),
                    "변경 사유"
            );

            PermissionGroupHistoryResponse result = service.saveDraft(groupId, request, testContext);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(result.name()).isEqualTo("Draft Name");
        }

        @Test
        @DisplayName("Given 초안이 존재 When saveDraft Then 기존 초안 업데이트")
        void givenDraftExists_whenSaveDraft_thenUpdatesDraft() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            PermissionGroup draft = PermissionGroup.createDraft(
                    root, 2, "Old Draft", "Old Desc", true,
                    List.of(), List.of(), null, "user", "User", now);

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.of(draft));

            PermissionGroupDraftRequest request = new PermissionGroupDraftRequest(
                    "Updated Draft",
                    "Updated Description",
                    false,
                    List.of(),
                    List.of(),
                    "업데이트 사유"
            );

            PermissionGroupHistoryResponse result = service.saveDraft(groupId, request, testContext);

            assertThat(result.name()).isEqualTo("Updated Draft");
            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("Given 초안 존재 When getDraft Then 초안 반환")
        void givenDraftExists_whenGetDraft_thenReturnsDraft() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            PermissionGroup draft = PermissionGroup.createDraft(
                    root, 2, "Draft", "Desc", true,
                    List.of(), List.of(), null, "user", "User", now);

            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.of(draft));

            PermissionGroupHistoryResponse result = service.getDraft(groupId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given 초안 없음 When getDraft Then 예외")
        void givenNoDraft_whenGetDraft_thenThrows() {
            UUID groupId = UUID.randomUUID();
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDraft(groupId))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("초안이 없습니다");
        }

        @Test
        @DisplayName("Given 초안 존재 When hasDraft Then true")
        void givenDraftExists_whenHasDraft_thenReturnsTrue() {
            UUID groupId = UUID.randomUUID();
            given(versionRepository.existsDraftByRootId(groupId)).willReturn(true);

            assertThat(service.hasDraft(groupId)).isTrue();
        }

        @Test
        @DisplayName("Given 초안 존재 When publishDraft Then 게시됨")
        void givenDraftExists_whenPublishDraft_thenPublished() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            PermissionGroup draft = PermissionGroup.createDraft(
                    root, 2, "Draft", "Desc", true,
                    List.of(), List.of(), null, "user", "User", now);

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.of(draft));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroupHistoryResponse result = service.publishDraft(groupId, testContext);

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            verify(permissionGroupService).evict(root.getGroupCode());
        }

        @Test
        @DisplayName("Given 초안 존재 When discardDraft Then 삭제됨")
        void givenDraftExists_whenDiscardDraft_thenDeleted() {
            UUID groupId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            PermissionGroup draft = PermissionGroup.createDraft(
                    root, 2, "Draft", "Desc", true,
                    List.of(), List.of(), null, "user", "User", now);

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.of(draft));

            service.discardDraft(groupId);

            verify(versionRepository).delete(draft);
        }

        @Test
        @DisplayName("Given 초안 없음 When discardDraft Then 예외")
        void givenNoDraft_whenDiscardDraft_thenThrows() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.discardDraft(groupId))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("삭제할 초안이 없습니다");
        }

        @Test
        @DisplayName("Given 초안 없음 When publishDraft Then 예외")
        void givenNoDraft_whenPublishDraft_thenThrows() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishDraft(groupId, testContext))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("게시할 초안이 없습니다");
        }

        @Test
        @DisplayName("Given 초안 없음 When hasDraft Then false")
        void givenNoDraft_whenHasDraft_thenReturnsFalse() {
            UUID groupId = UUID.randomUUID();
            given(versionRepository.existsDraftByRootId(groupId)).willReturn(false);

            assertThat(service.hasDraft(groupId)).isFalse();
        }

        @Test
        @DisplayName("Given assignments가 null When saveDraft Then 빈 리스트로 처리")
        void givenNullAssignments_whenSaveDraft_thenProcessedAsEmptyList() {
            UUID groupId = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(rootRepository.findById(groupId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(groupId)).willReturn(Optional.empty());
            given(versionRepository.findMaxVersionByRootId(groupId)).willReturn(1);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupDraftRequest request = new PermissionGroupDraftRequest(
                    "Draft Name",
                    "Draft Description",
                    true,
                    null,  // null assignments
                    List.of("GRP1"),
                    "변경 사유"
            );

            PermissionGroupHistoryResponse result = service.saveDraft(groupId, request, testContext);

            assertThat(result.assignments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("버전 생성 헬퍼")
    class VersionCreationHelperTest {

        @Test
        @DisplayName("Given root When createInitialVersion Then 첫 버전 생성")
        void givenRoot_whenCreateInitialVersion_thenCreatesVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("NEW", now);

            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    null, "New Group", "Description", true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("GRP1")
            );

            PermissionGroup result = service.createInitialVersion(root, request, testContext, now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.CREATE);
        }

        @Test
        @DisplayName("Given root When createDeleteVersion Then 삭제 버전 생성")
        void givenRoot_whenCreateDeleteVersion_thenCreatesDeleteVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(versionRepository.findMaxVersionByRootId(any())).willReturn(1);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroup result = service.createDeleteVersion(root, testContext, now);

            assertThat(result.isActive()).isFalse();
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.DELETE);
        }

        @Test
        @DisplayName("Given root When createRestoreVersion Then 복원 버전 생성")
        void givenRoot_whenCreateRestoreVersion_thenCreatesRestoreVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = createTestGroupRoot("TEST", "Test Group");

            given(versionRepository.findMaxVersionByRootId(any())).willReturn(1);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroup result = service.createRestoreVersion(root, testContext, now);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
        }

        @Test
        @DisplayName("Given root with no currentVersion When createDeleteVersion Then 버전 생성")
        void givenRootWithNoCurrentVersion_whenCreateDeleteVersion_thenCreatesVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("EMPTY", now);

            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroup result = service.createDeleteVersion(root, testContext, now);

            assertThat(result.isActive()).isFalse();
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.DELETE);
            assertThat(result.getName()).isNull();
        }

        @Test
        @DisplayName("Given root with no currentVersion When createRestoreVersion Then 버전 생성")
        void givenRootWithNoCurrentVersion_whenCreateRestoreVersion_thenCreatesVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("EMPTY", now);

            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);
            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doNothing().when(permissionGroupService).evict(anyString());

            PermissionGroup result = service.createRestoreVersion(root, testContext, now);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
            assertThat(result.getName()).isNull();
        }

        @Test
        @DisplayName("Given assignments가 빈 리스트 When createInitialVersion Then 빈 리스트로 처리")
        void givenEmptyAssignments_whenCreateInitialVersion_thenProcessedAsEmptyList() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("NEW", now);

            given(versionRepository.save(any(PermissionGroup.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    null, "New Group", "Description", true,
                    List.of(),  // 빈 리스트
                    List.of("GRP1")
            );

            PermissionGroup result = service.createInitialVersion(root, request, testContext, now);

            assertThat(result.getAssignments()).isEmpty();
        }
    }
}
