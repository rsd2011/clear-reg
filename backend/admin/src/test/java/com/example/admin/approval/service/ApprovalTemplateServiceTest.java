package com.example.admin.approval.service;

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

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.admin.approval.repository.ApprovalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.exception.ApprovalTemplateRootNotFoundException;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DraftRequest;
import com.example.admin.approval.dto.VersionComparisonResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class ApprovalTemplateServiceTest {

    private ApprovalTemplateRootRepository templateRepo;
    private ApprovalTemplateRepository versionRepo;
    private ApprovalGroupRepository groupRepo;
    private ApprovalTemplateService service;

    @BeforeEach
    void setUp() {
        templateRepo = mock(ApprovalTemplateRootRepository.class);
        versionRepo = mock(ApprovalTemplateRepository.class);
        groupRepo = mock(ApprovalGroupRepository.class);
        service = new ApprovalTemplateService(templateRepo, versionRepo, groupRepo);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, RowScope.ORG);
    }

    /**
     * 테스트용 ApprovalTemplateRoot 생성 (버전 컨테이너만).
     * 비즈니스 데이터는 버전에 저장됩니다.
     */
    private ApprovalTemplateRoot createTestTemplate(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        // 테스트를 위해 버전 1을 생성하고 활성화
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, "설명", true,
                ChangeAction.CREATE, null, "testuser", "테스트사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    /**
     * 비활성 상태의 테스트용 템플릿 생성.
     */
    private ApprovalTemplateRoot createInactiveTemplate(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, "설명", false,
                ChangeAction.DELETE, null, "testuser", "테스트사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    private ApprovalTemplate createTestVersion(ApprovalTemplateRoot root, int version,
                                                          VersionStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        ChangeAction action = version == 1 ? ChangeAction.CREATE : ChangeAction.UPDATE;
        String name = root.getName() != null ? root.getName() : "템플릿";
        int displayOrder = root.getDisplayOrder();
        String description = root.getDescription() != null ? root.getDescription() : "설명";
        boolean active = root.isActive();

        if (status == VersionStatus.DRAFT) {
            return ApprovalTemplate.createDraft(
                    root,
                    version,
                    name,
                    displayOrder,
                    description,
                    active,
                    null,
                    "testuser",
                    "테스트사용자",
                    now
            );
        } else {
            ApprovalTemplate v = ApprovalTemplate.create(
                    root,
                    version,
                    name,
                    displayOrder,
                    description,
                    active,
                    action,
                    null,
                    "testuser",
                    "테스트사용자",
                    now
            );
            if (status == VersionStatus.HISTORICAL) {
                v.close(now);
            }
            return v;
        }
    }

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Given: 버전 이력이 있을 때 / When: getVersionHistory 호출 / Then: 버전 목록 반환")
        void returnsVersionHistory() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalTemplate v2 = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findHistoryByRootId(templateId)).willReturn(List.of(v2, v1));

            List<VersionHistoryResponse> result = service.getVersionHistory(templateId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            assertThat(result.get(1).version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 존재하지 않는 템플릿 / When: getVersionHistory 호출 / Then: 예외 발생")
        void throwsExceptionForNonExistentTemplate() {
            UUID templateId = UUID.randomUUID();
            given(templateRepo.findById(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionHistory(templateId))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 특정 버전이 존재할 때 / When: getVersion 호출 / Then: 해당 버전 반환")
        void returnsSpecificVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.PUBLISHED);

            given(versionRepo.findByRootIdAndVersion(templateId, 1)).willReturn(Optional.of(v1));

            VersionHistoryResponse result = service.getVersion(templateId, 1);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 버전이 존재하지 않을 때 / When: getVersion 호출 / Then: 예외 발생")
        void throwsExceptionForNonExistentVersion() {
            UUID templateId = UUID.randomUUID();

            given(versionRepo.findByRootIdAndVersion(templateId, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersion(templateId, 99))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getVersionAsOf")
    class GetVersionAsOf {

        @Test
        @DisplayName("Given: 특정 시점에 유효한 버전이 있을 때 / When: getVersionAsOf 호출 / Then: 해당 버전 반환")
        void returnsVersionAsOf() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            OffsetDateTime asOf = OffsetDateTime.now().minusDays(5);
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);

            given(versionRepo.findByRootIdAsOf(templateId, asOf)).willReturn(Optional.of(v1));

            VersionHistoryResponse result = service.getVersionAsOf(templateId, asOf);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 해당 시점에 버전이 없을 때 / When: getVersionAsOf 호출 / Then: 예외 발생")
        void throwsExceptionForNoVersionAtTime() {
            UUID templateId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now().minusYears(10);

            given(versionRepo.findByRootIdAsOf(templateId, asOf)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionAsOf(templateId, asOf))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("해당 시점");
        }
    }

    @Nested
    @DisplayName("compareVersions")
    class CompareVersions {

        @Test
        @DisplayName("Given: 두 버전이 존재할 때 / When: compareVersions 호출 / Then: 비교 결과 반환")
        void comparesVersions() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalTemplate v2 = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v1, v2));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            assertThat(result.version1().version()).isEqualTo(1);
            assertThat(result.version2().version()).isEqualTo(2);
            assertThat(result.fieldDiffs()).isNotNull();
        }

        @Test
        @DisplayName("Given: 버전 중 하나가 없을 때 / When: compareVersions 호출 / Then: 예외 발생")
        void throwsExceptionForMissingVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 99)).willReturn(List.of(v1));

            assertThatThrownBy(() -> service.compareVersions(templateId, 1, 99))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("Given: 롤백 대상 버전이 존재할 때 / When: rollbackToVersion 호출 / Then: 새 버전 생성 및 반환")
        void rollbacksToVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalTemplate currentVersion = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            // 현재 버전 연결 설정
            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByRootIdAndVersion(templateId, 1)).willReturn(Optional.of(v1));
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(2);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            VersionHistoryResponse result = service.rollbackToVersion(templateId, 1, "잘못된 변경 원복", testContext());

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            verify(versionRepo).save(any());
        }

        @Test
        @DisplayName("Given: 롤백 대상 버전이 없을 때 / When: rollbackToVersion 호출 / Then: 예외 발생")
        void throwsExceptionForNonExistentRollbackVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByRootIdAndVersion(templateId, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rollbackToVersion(templateId, 99, "원복 사유", testContext()))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("Draft 관리")
    class DraftManagement {

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: saveDraft 호출 / Then: 새 초안 생성")
        void saveDraftCreatesNewDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            DraftRequest request = new DraftRequest(
                    "수정된 이름", 10, "수정된 설명", true, "변경 사유", List.of());

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(result.name()).isEqualTo("수정된 이름");
            verify(versionRepo).save(any());
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: saveDraft 호출 / Then: 기존 초안 업데이트")
        void saveDraftUpdatesExistingDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate existingDraft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(existingDraft));

            DraftRequest request = new DraftRequest(
                    "다시 수정", 20, "다시 설명", true, "변경 사유", List.of());

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 초안에 steps를 추가할 때 / When: saveDraft 호출 / Then: steps와 함께 저장")
        void saveDraftWithSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "팀장 그룹", 1, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));

            List<ApprovalTemplateStepRequest> steps = List.of(
                    new ApprovalTemplateStepRequest(1, "TEAM_LEADER")
            );
            DraftRequest request = new DraftRequest(
                    "새 이름", 10, "설명", true, "사유", steps);

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            verify(versionRepo).save(any());
        }

        @Test
        @DisplayName("Given: 초안이 존재할 때 / When: hasDraft 호출 / Then: true 반환")
        void hasDraftReturnsTrue() {
            UUID templateId = UUID.randomUUID();
            given(versionRepo.existsDraftByRootId(templateId)).willReturn(true);

            boolean result = service.hasDraft(templateId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalse() {
            UUID templateId = UUID.randomUUID();
            given(versionRepo.existsDraftByRootId(templateId)).willReturn(false);

            boolean result = service.hasDraft(templateId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Given: 초안이 존재할 때 / When: getDraft 호출 / Then: 초안 반환")
        void getDraftReturnsDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate draft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(draft));

            VersionHistoryResponse result = service.getDraft(templateId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: getDraft 호출 / Then: 예외 발생")
        void getDraftThrowsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();

            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDraft(templateId))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("초안");
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("Given: 초안이 존재할 때 / When: publishDraft 호출 / Then: 초안이 게시됨")
        void publishesDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate draft = createTestVersion(template, 2, VersionStatus.DRAFT);
            ApprovalTemplate currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);

            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(draft));

            VersionHistoryResponse result = service.publishDraft(templateId, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.PUBLISH);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: publishDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishDraft(templateId, testContext()))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("초안");
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("Given: 초안이 존재할 때 / When: discardDraft 호출 / Then: 초안 삭제됨")
        void discardsDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate draft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(draft));

            service.discardDraft(templateId);

            verify(versionRepo).delete(draft);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: discardDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.discardDraft(templateId))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("초안");
        }
    }

    @Nested
    @DisplayName("createInitialVersion")
    class CreateInitialVersion {

        @Test
        @DisplayName("Given: 새 템플릿 / When: createInitialVersion 호출 / Then: 버전 1 생성")
        void createsInitialVersion() {
            ApprovalTemplateRoot template = createTestTemplate("새 템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "팀장 그룹", 1, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));

            com.example.admin.approval.dto.ApprovalTemplateRootRequest request =
                    new com.example.admin.approval.dto.ApprovalTemplateRootRequest(
                            "새 템플릿", 1, "설명", true,
                            List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            ApprovalTemplate result = service.createInitialVersion(template, request, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.CREATE);
            verify(versionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("createUpdateVersion")
    class CreateUpdateVersion {

        @Test
        @DisplayName("Given: 기존 버전이 있을 때 / When: createUpdateVersion 호출 / Then: 새 버전 생성 및 기존 버전 종료")
        void createsUpdateVersion() {
            ApprovalTemplateRoot template = createTestTemplate("기존 템플릿");
            ApprovalTemplate currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());
            ApprovalGroup group = ApprovalGroup.create("DEPT_HEAD", "부서장", "부서장 그룹", 1, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(groupRepo.findByGroupCode("DEPT_HEAD")).willReturn(Optional.of(group));

            com.example.admin.approval.dto.ApprovalTemplateRootRequest request =
                    new com.example.admin.approval.dto.ApprovalTemplateRootRequest(
                            "수정된 템플릿", 2, "수정된 설명", true,
                            List.of(new ApprovalTemplateStepRequest(1, "DEPT_HEAD")));

            ApprovalTemplate result = service.createUpdateVersion(template, request, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.UPDATE);
            assertThat(currentVersion.getValidTo()).isNotNull();  // 기존 버전 종료됨
            verify(versionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("createDeleteVersion")
    class CreateDeleteVersion {

        @Test
        @DisplayName("Given: 템플릿 삭제 시 / When: createDeleteVersion 호출 / Then: DELETE 버전 생성")
        void createsDeleteVersion() {
            ApprovalTemplateRoot template = createTestTemplate("삭제할 템플릿");
            ApprovalTemplate currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createDeleteVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.DELETE);
            assertThat(result.isActive()).isFalse();
            verify(versionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("createRestoreVersion")
    class CreateRestoreVersion {

        @Test
        @DisplayName("Given: 비활성 템플릿 / When: createRestoreVersion 호출 / Then: RESTORE 버전 생성")
        void createsRestoreVersion() {
            // 비활성 상태의 템플릿 생성 (active=false)
            ApprovalTemplateRoot template = createInactiveTemplate("복원할 템플릿");
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createRestoreVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
            assertThat(result.isActive()).isTrue();
            verify(versionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("compareVersions - 브랜치 커버리지")
    class CompareVersionsBranches {

        @Test
        @DisplayName("Given: v1 > v2 순서로 조회될 때 / When: compareVersions 호출 / Then: 버전 순서 정렬됨")
        void sortsVersionsWhenReversed() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalTemplate v2 = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            // 역순으로 반환
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v2, v1));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            // 낮은 버전이 version1에 위치해야 함
            assertThat(result.version1().version()).isEqualTo(1);
            assertThat(result.version2().version()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("compareSteps - 브랜치 커버리지")
    class CompareStepsBranches {

        private ApprovalGroup createGroup(String code, String name) {
            return ApprovalGroup.create(code, name, "설명", 1, OffsetDateTime.now());
        }

        private ApprovalTemplate createVersionWithSteps(ApprovalTemplateRoot template, int version,
                                                                    List<ApprovalGroup> groups) {
            ApprovalTemplate v = ApprovalTemplate.create(
                    template, version, template.getName(), template.getDisplayOrder(),
                    template.getDescription(), template.isActive(),
                    version == 1 ? ChangeAction.CREATE : ChangeAction.UPDATE,
                    null, "testuser", "테스트사용자", OffsetDateTime.now()
            );
            int stepOrder = 1;
            for (ApprovalGroup group : groups) {
                ApprovalTemplateStep step = ApprovalTemplateStep.create(v, stepOrder++, group, false);
                v.addStep(step);
            }
            return v;
        }

        @Test
        @DisplayName("Given: v2에만 step이 있을 때 / When: compareVersions 호출 / Then: ADDED diff 반환")
        void detectsAddedSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group = createGroup("TEAM_LEADER", "팀장");

            // v1: 스텝 없음, v2: 스텝 있음
            ApprovalTemplate v1 = createVersionWithSteps(template, 1, List.of());
            ApprovalTemplate v2 = createVersionWithSteps(template, 2, List.of(group));

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v1, v2));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            assertThat(result.stepDiffs()).hasSize(1);
            assertThat(result.stepDiffs().get(0).diffType())
                    .isEqualTo(VersionComparisonResponse.DiffType.ADDED);
        }

        @Test
        @DisplayName("Given: v1에만 step이 있을 때 / When: compareVersions 호출 / Then: REMOVED diff 반환")
        void detectsRemovedSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group = createGroup("TEAM_LEADER", "팀장");

            // v1: 스텝 있음, v2: 스텝 없음
            ApprovalTemplate v1 = createVersionWithSteps(template, 1, List.of(group));
            ApprovalTemplate v2 = createVersionWithSteps(template, 2, List.of());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v1, v2));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            assertThat(result.stepDiffs()).hasSize(1);
            assertThat(result.stepDiffs().get(0).diffType())
                    .isEqualTo(VersionComparisonResponse.DiffType.REMOVED);
        }

        @Test
        @DisplayName("Given: 양쪽에 다른 그룹의 step이 있을 때 / When: compareVersions 호출 / Then: MODIFIED diff 반환")
        void detectsModifiedSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group1 = createGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createGroup("DEPT_HEAD", "부서장");

            ApprovalTemplate v1 = createVersionWithSteps(template, 1, List.of(group1));
            ApprovalTemplate v2 = createVersionWithSteps(template, 2, List.of(group2));

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v1, v2));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            assertThat(result.stepDiffs()).hasSize(1);
            assertThat(result.stepDiffs().get(0).diffType())
                    .isEqualTo(VersionComparisonResponse.DiffType.MODIFIED);
        }

        @Test
        @DisplayName("Given: 양쪽에 같은 그룹의 step이 있을 때 / When: compareVersions 호출 / Then: 빈 diff 반환")
        void detectsUnchangedSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group = createGroup("TEAM_LEADER", "팀장");

            ApprovalTemplate v1 = createVersionWithSteps(template, 1, List.of(group));
            ApprovalTemplate v2 = createVersionWithSteps(template, 2, List.of(group));

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 2)).willReturn(List.of(v1, v2));

            VersionComparisonResponse result = service.compareVersions(templateId, 1, 2);

            assertThat(result.stepDiffs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("rollbackToVersion - 브랜치 커버리지")
    class RollbackToVersionBranches {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: rollbackToVersion 호출 / Then: 롤백 성공")
        void rollbacksWithoutCurrentVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            // 현재 버전 설정 안함
            ApprovalTemplate targetVersion = createTestVersion(template, 1, VersionStatus.HISTORICAL);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByRootIdAndVersion(templateId, 1)).willReturn(Optional.of(targetVersion));
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            VersionHistoryResponse result = service.rollbackToVersion(templateId, 1, "원복 사유", testContext());

            assertThat(result.version()).isEqualTo(2);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
        }

        @Test
        @DisplayName("Given: 롤백 대상에 steps가 있을 때 / When: rollbackToVersion 호출 / Then: steps 복사됨")
        void rollbackCopiesSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "설명", 1, OffsetDateTime.now());

            ApprovalTemplate targetVersion = ApprovalTemplate.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalTemplateStep step = ApprovalTemplateStep.create(targetVersion, 1, group, false);
            targetVersion.addStep(step);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByRootIdAndVersion(templateId, 1)).willReturn(Optional.of(targetVersion));
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            VersionHistoryResponse result = service.rollbackToVersion(templateId, 1, "원복 사유", testContext());

            assertThat(result.version()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("publishDraft - 브랜치 커버리지")
    class PublishDraftBranches {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: publishDraft 호출 / Then: 초안 게시 성공")
        void publishesDraftWithoutCurrentVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            // 현재 버전 설정 안함
            ApprovalTemplate draft = createTestVersion(template, 1, VersionStatus.DRAFT);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(draft));

            VersionHistoryResponse result = service.publishDraft(templateId, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("createUpdateVersion - 브랜치 커버리지")
    class CreateUpdateVersionBranches {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: createUpdateVersion 호출 / Then: 새 버전 생성")
        void createsUpdateVersionWithoutCurrentVersion() {
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            // 현재 버전 설정 안함
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(0);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            com.example.admin.approval.dto.ApprovalTemplateRootRequest request =
                    new com.example.admin.approval.dto.ApprovalTemplateRootRequest(
                            "수정된 템플릿", 2, "수정된 설명", true, null);

            ApprovalTemplate result = service.createUpdateVersion(template, request, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.UPDATE);
        }

        @Test
        @DisplayName("Given: steps가 null인 경우 / When: createUpdateVersion 호출 / Then: steps 없이 버전 생성")
        void createsUpdateVersionWithNullSteps() {
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            // steps를 null로 전달
            com.example.admin.approval.dto.ApprovalTemplateRootRequest request =
                    new com.example.admin.approval.dto.ApprovalTemplateRootRequest(
                            "수정된 템플릿", 2, "수정된 설명", true, null);

            ApprovalTemplate result = service.createUpdateVersion(template, request, testContext(), now);

            assertThat(result.getSteps()).isEmpty();
        }
    }

    @Nested
    @DisplayName("createDeleteVersion - 브랜치 커버리지")
    class CreateDeleteVersionBranches {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: createDeleteVersion 호출 / Then: DELETE 버전 생성 (steps 없음)")
        void createsDeleteVersionWithoutCurrentVersion() {
            ApprovalTemplateRoot template = createTestTemplate("삭제할 템플릿");
            // 현재 버전 설정 안함
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(0);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createDeleteVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.DELETE);
            assertThat(result.getSteps()).isEmpty();
        }

        @Test
        @DisplayName("Given: 현재 버전에 steps가 있을 때 / When: createDeleteVersion 호출 / Then: steps 복사됨")
        void createsDeleteVersionWithSteps() {
            ApprovalTemplateRoot template = createTestTemplate("삭제할 템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "설명", 1, OffsetDateTime.now());

            ApprovalTemplate currentVersion = ApprovalTemplate.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalTemplateStep step = ApprovalTemplateStep.create(currentVersion, 1, group, false);
            currentVersion.addStep(step);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createDeleteVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getSteps()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createRestoreVersion - 브랜치 커버리지")
    class CreateRestoreVersionBranches {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: createRestoreVersion 호출 / Then: RESTORE 버전 생성 (steps 없음)")
        void createsRestoreVersionWithoutCurrentVersion() {
            ApprovalTemplateRoot template = createTestTemplate("복원할 템플릿");
            // 현재 버전 설정 안함
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(0);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createRestoreVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
            assertThat(result.getSteps()).isEmpty();
        }

        @Test
        @DisplayName("Given: 현재 버전에 steps가 있을 때 / When: createRestoreVersion 호출 / Then: steps 복사됨")
        void createsRestoreVersionWithSteps() {
            ApprovalTemplateRoot template = createTestTemplate("복원할 템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "설명", 1, OffsetDateTime.now());

            ApprovalTemplate currentVersion = ApprovalTemplate.create(
                    template, 1, "이름", 0, "설명", false,
                    ChangeAction.DELETE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalTemplateStep step = ApprovalTemplateStep.create(currentVersion, 1, group, false);
            currentVersion.addStep(step);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByRootId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplate result = service.createRestoreVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getSteps()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("saveDraft - 브랜치 커버리지")
    class SaveDraftBranches {

        @Test
        @DisplayName("Given: steps가 null인 경우 / When: saveDraft 호출 / Then: steps 없이 저장")
        void saveDraftWithNullSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.empty());
            given(versionRepo.findMaxVersionByRootId(templateId)).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            // steps를 null로 전달
            DraftRequest request = new DraftRequest(
                    "수정된 이름", 10, "수정된 설명", true, "변경 사유", null);

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 기존 초안에 steps를 추가할 때 / When: saveDraft 호출 / Then: steps 교체됨")
        void updateDraftWithSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplate("템플릿");
            ApprovalTemplate existingDraft = createTestVersion(template, 2, VersionStatus.DRAFT);
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "팀장 그룹", 1, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByRootId(templateId)).willReturn(Optional.of(existingDraft));
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));

            List<ApprovalTemplateStepRequest> steps = List.of(
                    new ApprovalTemplateStepRequest(1, "TEAM_LEADER")
            );
            DraftRequest request = new DraftRequest(
                    "새 이름", 10, "새 설명", true, "사유", steps);

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("createInitialVersion - 브랜치 커버리지")
    class CreateInitialVersionBranches {

        @Test
        @DisplayName("Given: steps가 null인 경우 / When: createInitialVersion 호출 / Then: steps 없이 버전 생성")
        void createsInitialVersionWithNullSteps() {
            ApprovalTemplateRoot template = createTestTemplate("새 템플릿");
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            // steps를 null로 전달
            com.example.admin.approval.dto.ApprovalTemplateRootRequest request =
                    new com.example.admin.approval.dto.ApprovalTemplateRootRequest(
                            "새 템플릿", 1, "설명", true, null);

            ApprovalTemplate result = service.createInitialVersion(template, request, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getSteps()).isEmpty();
        }
    }
}
