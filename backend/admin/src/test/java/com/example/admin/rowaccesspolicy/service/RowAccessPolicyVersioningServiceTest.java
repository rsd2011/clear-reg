package com.example.admin.rowaccesspolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyDraftRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyHistoryResponse;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootRequest;
import com.example.admin.rowaccesspolicy.exception.RowAccessPolicyRootNotFoundException;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRootRepository;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("RowAccessPolicyVersioningService")
class RowAccessPolicyVersioningServiceTest {

    private RowAccessPolicyRootRepository rootRepository;
    private RowAccessPolicyRepository versionRepository;
    private RowAccessPolicyVersioningService service;

    @BeforeEach
    void setUp() {
        rootRepository = mock(RowAccessPolicyRootRepository.class);
        versionRepository = mock(RowAccessPolicyRepository.class);
        service = new RowAccessPolicyVersioningService(rootRepository, versionRepository);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, List.of());
    }

    private RowAccessPolicyRoot createTestRoot() {
        return RowAccessPolicyRoot.create(OffsetDateTime.now());
    }

    private RowAccessPolicy createTestVersion(RowAccessPolicyRoot root, int versionNumber) {
        return RowAccessPolicy.create(
                root, versionNumber, "정책", "설명",
                FeatureCode.DRAFT, null, null, null,
                RowScope.OWN, 100, true,
                null, null,
                ChangeAction.CREATE, null, "user", "사용자",
                OffsetDateTime.now());
    }

    private RowAccessPolicy createTestDraft(RowAccessPolicyRoot root, int versionNumber) {
        return RowAccessPolicy.createDraft(
                root, versionNumber, "초안", "설명",
                FeatureCode.DRAFT, null, null, null,
                RowScope.OWN, 100, true,
                null, null,
                null, "user", "사용자",
                OffsetDateTime.now());
    }

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Given: 버전 이력이 있는 정책 / When: getVersionHistory 호출 / Then: 버전 목록 반환")
        void getVersionHistoryReturnsVersions() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            RowAccessPolicy v2 = createTestVersion(root, 2);

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findHistoryByRootId(policyId)).willReturn(List.of(v2, v1));

            List<RowAccessPolicyHistoryResponse> result = service.getVersionHistory(policyId);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 존재하지 않는 정책 / When: getVersionHistory 호출 / Then: 예외 발생")
        void throwsExceptionWhenPolicyNotFound() {
            UUID policyId = UUID.randomUUID();
            given(rootRepository.findById(policyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionHistory(policyId))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 존재하는 버전 / When: getVersion 호출 / Then: 버전 반환")
        void getVersionReturnsVersion() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root, 1);

            given(versionRepository.findByRootIdAndVersion(policyId, 1)).willReturn(Optional.of(version));

            RowAccessPolicyHistoryResponse result = service.getVersion(policyId, 1);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 존재하지 않는 버전 / When: getVersion 호출 / Then: 예외 발생")
        void throwsExceptionWhenVersionNotFound() {
            UUID policyId = UUID.randomUUID();
            given(versionRepository.findByRootIdAndVersion(policyId, 999)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersion(policyId, 999))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getVersionAsOf")
    class GetVersionAsOf {

        @Test
        @DisplayName("Given: 해당 시점에 유효한 버전 / When: getVersionAsOf 호출 / Then: 버전 반환")
        void getVersionAsOfReturnsVersion() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root, 1);
            OffsetDateTime asOf = OffsetDateTime.now();

            given(versionRepository.findByRootIdAsOf(policyId, asOf)).willReturn(Optional.of(version));

            RowAccessPolicyHistoryResponse result = service.getVersionAsOf(policyId, asOf);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 해당 시점에 유효한 버전 없음 / When: getVersionAsOf 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoVersionAtTime() {
            UUID policyId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now();
            given(versionRepository.findByRootIdAsOf(policyId, asOf)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionAsOf(policyId, asOf))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("유효한 버전이 없습니다");
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("Given: 롤백 대상 버전 / When: rollbackToVersion 호출 / Then: 롤백 버전 생성")
        void rollbackCreatesNewVersion() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            RowAccessPolicy v2 = createTestVersion(root, 2);
            root.activateNewVersion(v1, OffsetDateTime.now());
            root.activateNewVersion(v2, OffsetDateTime.now());

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findByRootIdAndVersion(policyId, 1)).willReturn(Optional.of(v1));
            given(versionRepository.findMaxVersionByRootId(policyId)).willReturn(2);
            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicyHistoryResponse result = service.rollbackToVersion(
                    policyId, 1, "롤백 사유", testContext());

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(result.rollbackFromVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 존재하지 않는 롤백 대상 버전 / When: rollbackToVersion 호출 / Then: 예외 발생")
        void throwsExceptionWhenTargetVersionNotFound() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findByRootIdAndVersion(policyId, 999)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rollbackToVersion(policyId, 999, "사유", testContext()))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("롤백할 버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("saveDraft")
    class SaveDraft {

        @Test
        @DisplayName("Given: 기존 초안 없음 / When: saveDraft 호출 / Then: 새 초안 생성")
        void saveDraftCreatesNewDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.empty());
            given(versionRepository.findMaxVersionByRootId(policyId)).willReturn(1);
            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicyDraftRequest request = new RowAccessPolicyDraftRequest(
                    "초안", "설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.OWN, 100, true,
                    null, null, null);

            RowAccessPolicyHistoryResponse result = service.saveDraft(policyId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            verify(versionRepository).save(any());
        }

        @Test
        @DisplayName("Given: 기존 초안 있음 / When: saveDraft 호출 / Then: 기존 초안 수정")
        void saveDraftUpdatesExistingDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy existingDraft = createTestDraft(root, 2);

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.of(existingDraft));

            RowAccessPolicyDraftRequest request = new RowAccessPolicyDraftRequest(
                    "수정된 초안", "수정된 설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.ALL, 50, true,
                    null, null, "수정 사유");

            RowAccessPolicyHistoryResponse result = service.saveDraft(policyId, request, testContext());

            assertThat(result.name()).isEqualTo("수정된 초안");
            assertThat(result.description()).isEqualTo("수정된 설명");
        }
    }

    @Nested
    @DisplayName("getDraft")
    class GetDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: getDraft 호출 / Then: 초안 반환")
        void getDraftReturnsDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy draft = createTestDraft(root, 2);

            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.of(draft));

            RowAccessPolicyHistoryResponse result = service.getDraft(policyId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: getDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID policyId = UUID.randomUUID();
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDraft(policyId))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("초안이 없습니다");
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: hasDraft 호출 / Then: true 반환")
        void hasDraftReturnsTrue() {
            UUID policyId = UUID.randomUUID();
            given(versionRepository.existsDraftByRootId(policyId)).willReturn(true);

            boolean result = service.hasDraft(policyId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalse() {
            UUID policyId = UUID.randomUUID();
            given(versionRepository.existsDraftByRootId(policyId)).willReturn(false);

            boolean result = service.hasDraft(policyId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: publishDraft 호출 / Then: 초안 게시")
        void publishDraftPublishesDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            RowAccessPolicy draft = createTestDraft(root, 2);

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.of(draft));

            RowAccessPolicyHistoryResponse result = service.publishDraft(policyId, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.PUBLISH);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: publishDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishDraft(policyId, testContext()))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("게시할 초안이 없습니다");
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: discardDraft 호출 / Then: 초안 삭제")
        void discardDraftDeletesDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy draft = createTestDraft(root, 2);

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.of(draft));

            service.discardDraft(policyId);

            verify(versionRepository).delete(draft);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: discardDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyRoot root = createTestRoot();

            given(rootRepository.findById(policyId)).willReturn(Optional.of(root));
            given(versionRepository.findDraftByRootId(policyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.discardDraft(policyId))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("삭제할 초안이 없습니다");
        }
    }

    @Nested
    @DisplayName("createInitialVersion")
    class CreateInitialVersion {

        @Test
        @DisplayName("Given: 유효한 요청 / When: createInitialVersion 호출 / Then: 첫 번째 버전 생성")
        void createsInitialVersion() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "정책", "설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.OWN, 100, true, null, null);

            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicy result = service.createInitialVersion(
                    root, request, testContext(), OffsetDateTime.now());

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.CREATE);
            assertThat(root.getCurrentVersion()).isEqualTo(result);
        }
    }

    @Nested
    @DisplayName("createUpdateVersion")
    class CreateUpdateVersion {

        @Test
        @DisplayName("Given: 기존 버전 / When: createUpdateVersion 호출 / Then: 새 버전 생성 및 기존 버전 종료")
        void createsUpdateVersion() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "수정된 정책", "수정된 설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.ALL, 50, true, null, null);

            given(versionRepository.findMaxVersionByRootId(root.getId())).willReturn(1);
            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicy result = service.createUpdateVersion(
                    root, request, testContext(), OffsetDateTime.now());

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.UPDATE);
            assertThat(v1.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
        }
    }

    @Nested
    @DisplayName("createDeleteVersion")
    class CreateDeleteVersion {

        @Test
        @DisplayName("Given: 기존 버전 / When: createDeleteVersion 호출 / Then: 비활성 버전 생성")
        void createsDeleteVersion() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            given(versionRepository.findMaxVersionByRootId(root.getId())).willReturn(1);
            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicy result = service.createDeleteVersion(
                    root, testContext(), OffsetDateTime.now());

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.DELETE);
            assertThat(result.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("createRestoreVersion")
    class CreateRestoreVersion {

        @Test
        @DisplayName("Given: 비활성 버전 / When: createRestoreVersion 호출 / Then: 활성 버전 생성")
        void createsRestoreVersion() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy v1 = createTestVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            given(versionRepository.findMaxVersionByRootId(root.getId())).willReturn(1);
            given(versionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicy result = service.createRestoreVersion(
                    root, testContext(), OffsetDateTime.now());

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
            assertThat(result.isActive()).isTrue();
        }
    }
}
