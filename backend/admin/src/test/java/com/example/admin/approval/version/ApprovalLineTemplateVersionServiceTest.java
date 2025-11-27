package com.example.admin.approval.version;

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

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalLineTemplateNotFoundException;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DraftRequest;
import com.example.admin.approval.dto.RollbackRequest;
import com.example.admin.approval.dto.VersionComparisonResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class ApprovalLineTemplateVersionServiceTest {

    private ApprovalLineTemplateRepository templateRepo;
    private ApprovalLineTemplateVersionRepository versionRepo;
    private ApprovalGroupRepository groupRepo;
    private ApprovalLineTemplateVersionService service;

    @BeforeEach
    void setUp() {
        templateRepo = mock(ApprovalLineTemplateRepository.class);
        versionRepo = mock(ApprovalLineTemplateVersionRepository.class);
        groupRepo = mock(ApprovalGroupRepository.class);
        service = new ApprovalLineTemplateVersionService(templateRepo, versionRepo, groupRepo);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, RowScope.ORG);
    }

    private ApprovalLineTemplate createTestTemplate(String name) {
        return ApprovalLineTemplate.create(name, 0, "설명", OffsetDateTime.now());
    }

    private ApprovalLineTemplateVersion createTestVersion(ApprovalLineTemplate template, int version,
                                                          VersionStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        ChangeAction action = version == 1 ? ChangeAction.CREATE : ChangeAction.UPDATE;

        if (status == VersionStatus.DRAFT) {
            return ApprovalLineTemplateVersion.createDraft(
                    template,
                    version,
                    template.getName(),
                    template.getDisplayOrder(),
                    template.getDescription(),
                    template.isActive(),
                    null,
                    "testuser",
                    "테스트사용자",
                    now
            );
        } else {
            ApprovalLineTemplateVersion v = ApprovalLineTemplateVersion.create(
                    template,
                    version,
                    template.getName(),
                    template.getDisplayOrder(),
                    template.getDescription(),
                    template.isActive(),
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalLineTemplateVersion v2 = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findHistoryByTemplateId(templateId)).willReturn(List.of(v2, v1));

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
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 특정 버전이 존재할 때 / When: getVersion 호출 / Then: 해당 버전 반환")
        void returnsSpecificVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.PUBLISHED);

            given(versionRepo.findByTemplateIdAndVersion(templateId, 1)).willReturn(Optional.of(v1));

            VersionHistoryResponse result = service.getVersion(templateId, 1);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 버전이 존재하지 않을 때 / When: getVersion 호출 / Then: 예외 발생")
        void throwsExceptionForNonExistentVersion() {
            UUID templateId = UUID.randomUUID();

            given(versionRepo.findByTemplateIdAndVersion(templateId, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersion(templateId, 99))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            OffsetDateTime asOf = OffsetDateTime.now().minusDays(5);
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);

            given(versionRepo.findByTemplateIdAsOf(templateId, asOf)).willReturn(Optional.of(v1));

            VersionHistoryResponse result = service.getVersionAsOf(templateId, asOf);

            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: 해당 시점에 버전이 없을 때 / When: getVersionAsOf 호출 / Then: 예외 발생")
        void throwsExceptionForNoVersionAtTime() {
            UUID templateId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now().minusYears(10);

            given(versionRepo.findByTemplateIdAsOf(templateId, asOf)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVersionAsOf(templateId, asOf))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalLineTemplateVersion v2 = createTestVersion(template, 2, VersionStatus.PUBLISHED);

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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findVersionsForComparison(templateId, 1, 99)).willReturn(List.of(v1));

            assertThatThrownBy(() -> service.compareVersions(templateId, 1, 99))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion v1 = createTestVersion(template, 1, VersionStatus.HISTORICAL);
            ApprovalLineTemplateVersion currentVersion = createTestVersion(template, 2, VersionStatus.PUBLISHED);

            // 현재 버전 연결 설정
            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByTemplateIdAndVersion(templateId, 1)).willReturn(Optional.of(v1));
            given(versionRepo.findMaxVersionByTemplateId(templateId)).willReturn(2);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RollbackRequest request = new RollbackRequest(1, "잘못된 변경 원복");

            VersionHistoryResponse result = service.rollbackToVersion(templateId, request, testContext());

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            verify(versionRepo).save(any());
        }

        @Test
        @DisplayName("Given: 롤백 대상 버전이 없을 때 / When: rollbackToVersion 호출 / Then: 예외 발생")
        void throwsExceptionForNonExistentRollbackVersion() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findByTemplateIdAndVersion(templateId, 99)).willReturn(Optional.empty());

            RollbackRequest request = new RollbackRequest(99, "원복 사유");

            assertThatThrownBy(() -> service.rollbackToVersion(templateId, request, testContext()))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.empty());
            given(versionRepo.findMaxVersionByTemplateId(templateId)).willReturn(1);
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion existingDraft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.of(existingDraft));

            DraftRequest request = new DraftRequest(
                    "다시 수정", 20, "다시 설명", true, "변경 사유", List.of());

            VersionHistoryResponse result = service.saveDraft(templateId, request, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 초안에 steps를 추가할 때 / When: saveDraft 호출 / Then: steps와 함께 저장")
        void saveDraftWithSteps() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "팀장 그룹", 1, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.empty());
            given(versionRepo.findMaxVersionByTemplateId(templateId)).willReturn(1);
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
            given(versionRepo.existsDraftByTemplateId(templateId)).willReturn(true);

            boolean result = service.hasDraft(templateId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalse() {
            UUID templateId = UUID.randomUUID();
            given(versionRepo.existsDraftByTemplateId(templateId)).willReturn(false);

            boolean result = service.hasDraft(templateId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Given: 초안이 존재할 때 / When: getDraft 호출 / Then: 초안 반환")
        void getDraftReturnsDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion draft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.of(draft));

            VersionHistoryResponse result = service.getDraft(templateId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: getDraft 호출 / Then: 예외 발생")
        void getDraftThrowsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();

            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDraft(templateId))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion draft = createTestVersion(template, 2, VersionStatus.DRAFT);
            ApprovalLineTemplateVersion currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);

            template.activateNewVersion(currentVersion, OffsetDateTime.now());

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.of(draft));

            VersionHistoryResponse result = service.publishDraft(templateId, testContext());

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.PUBLISH);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: publishDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishDraft(templateId, testContext()))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
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
            ApprovalLineTemplate template = createTestTemplate("템플릿");
            ApprovalLineTemplateVersion draft = createTestVersion(template, 2, VersionStatus.DRAFT);

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.of(draft));

            service.discardDraft(templateId);

            verify(versionRepo).delete(draft);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: discardDraft 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoDraft() {
            UUID templateId = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("템플릿");

            given(templateRepo.findById(templateId)).willReturn(Optional.of(template));
            given(versionRepo.findDraftByTemplateId(templateId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.discardDraft(templateId))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
                    .hasMessageContaining("초안");
        }
    }

    @Nested
    @DisplayName("createInitialVersion")
    class CreateInitialVersion {

        @Test
        @DisplayName("Given: 새 템플릿 / When: createInitialVersion 호출 / Then: 버전 1 생성")
        void createsInitialVersion() {
            ApprovalLineTemplate template = createTestTemplate("새 템플릿");
            ApprovalGroup group = ApprovalGroup.create("TEAM_LEADER", "팀장", "팀장 그룹", 1, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));

            com.example.admin.approval.dto.ApprovalLineTemplateRequest request =
                    new com.example.admin.approval.dto.ApprovalLineTemplateRequest(
                            "새 템플릿", 1, "설명", true,
                            List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            ApprovalLineTemplateVersion result = service.createInitialVersion(template, request, testContext(), now);

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
            ApprovalLineTemplate template = createTestTemplate("기존 템플릿");
            ApprovalLineTemplateVersion currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());
            ApprovalGroup group = ApprovalGroup.create("DEPT_HEAD", "부서장", "부서장 그룹", 1, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByTemplateId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(groupRepo.findByGroupCode("DEPT_HEAD")).willReturn(Optional.of(group));

            com.example.admin.approval.dto.ApprovalLineTemplateRequest request =
                    new com.example.admin.approval.dto.ApprovalLineTemplateRequest(
                            "수정된 템플릿", 2, "수정된 설명", true,
                            List.of(new ApprovalTemplateStepRequest(1, "DEPT_HEAD")));

            ApprovalLineTemplateVersion result = service.createUpdateVersion(template, request, testContext(), now);

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
            ApprovalLineTemplate template = createTestTemplate("삭제할 템플릿");
            ApprovalLineTemplateVersion currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByTemplateId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalLineTemplateVersion result = service.createDeleteVersion(template, testContext(), now);

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
            ApprovalLineTemplate template = createTestTemplate("복원할 템플릿");
            template.rename("복원할 템플릿", 0, "설명", false, OffsetDateTime.now());
            ApprovalLineTemplateVersion currentVersion = createTestVersion(template, 1, VersionStatus.PUBLISHED);
            template.activateNewVersion(currentVersion, OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.findMaxVersionByTemplateId(template.getId())).willReturn(1);
            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalLineTemplateVersion result = service.createRestoreVersion(template, testContext(), now);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.RESTORE);
            assertThat(result.isActive()).isTrue();
            verify(versionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("createCopyVersion")
    class CreateCopyVersion {

        @Test
        @DisplayName("Given: 원본 템플릿 / When: createCopyVersion 호출 / Then: COPY 버전 생성")
        void createsCopyVersion() {
            ApprovalLineTemplate sourceTemplate = createTestTemplate("원본 템플릿");
            ApprovalLineTemplateVersion sourceVersion = createTestVersion(sourceTemplate, 1, VersionStatus.PUBLISHED);
            sourceTemplate.activateNewVersion(sourceVersion, OffsetDateTime.now());

            ApprovalLineTemplate newTemplate = createTestTemplate("복사된 템플릿");
            OffsetDateTime now = OffsetDateTime.now();

            given(versionRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalLineTemplateVersion result = service.createCopyVersion(
                    newTemplate, sourceTemplate, "복사된 템플릿", "복사 설명", testContext(), now);

            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getChangeAction()).isEqualTo(ChangeAction.COPY);
            assertThat(result.getName()).isEqualTo("복사된 템플릿");
            verify(versionRepo).save(any());
        }
    }
}
