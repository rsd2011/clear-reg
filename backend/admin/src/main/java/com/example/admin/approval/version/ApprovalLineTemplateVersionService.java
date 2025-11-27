package com.example.admin.approval.version;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalGroupNotFoundException;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalLineTemplateNotFoundException;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DraftRequest;
import com.example.admin.approval.dto.RollbackRequest;
import com.example.admin.approval.dto.VersionComparisonResponse;
import com.example.admin.approval.dto.VersionComparisonResponse.StepDiff;
import com.example.admin.approval.dto.VersionComparisonResponse.VersionSummary;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.permission.context.AuthContext;
import com.example.common.version.ChangeAction;
import com.example.common.version.FieldDiff;
import com.example.common.version.ObjectDiffUtils;

/**
 * 승인선 템플릿 버전 관리 서비스 (SCD Type 2).
 */
@Service
@Transactional
public class ApprovalLineTemplateVersionService {

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "name", "이름",
            "displayOrder", "표시순서",
            "description", "설명",
            "active", "활성화"
    );

    private final ApprovalLineTemplateRepository templateRepository;
    private final ApprovalLineTemplateVersionRepository versionRepository;
    private final ApprovalGroupRepository groupRepository;

    public ApprovalLineTemplateVersionService(ApprovalLineTemplateRepository templateRepository,
                                              ApprovalLineTemplateVersionRepository versionRepository,
                                              ApprovalGroupRepository groupRepository) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.groupRepository = groupRepository;
    }

    // ==========================================================================
    // 버전 이력 조회
    // ==========================================================================

    /**
     * 버전 이력 목록 조회 (최신순, Draft 제외).
     */
    @Transactional(readOnly = true)
    public List<VersionHistoryResponse> getVersionHistory(UUID templateId) {
        findTemplateOrThrow(templateId);

        return versionRepository.findHistoryByTemplateId(templateId).stream()
                .map(VersionHistoryResponse::from)
                .toList();
    }

    /**
     * 특정 버전 상세 조회.
     */
    @Transactional(readOnly = true)
    public VersionHistoryResponse getVersion(UUID templateId, Integer versionNumber) {
        ApprovalLineTemplateVersion version = versionRepository
                .findByTemplateIdAndVersion(templateId, versionNumber)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException(
                        "버전을 찾을 수 없습니다: " + versionNumber));

        return VersionHistoryResponse.from(version);
    }

    /**
     * 특정 시점의 버전 조회 (Point-in-Time Query).
     */
    @Transactional(readOnly = true)
    public VersionHistoryResponse getVersionAsOf(UUID templateId, OffsetDateTime asOf) {
        ApprovalLineTemplateVersion version = versionRepository
                .findByTemplateIdAsOf(templateId, asOf)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException(
                        "해당 시점에 유효한 버전이 없습니다: " + asOf));

        return VersionHistoryResponse.from(version);
    }

    // ==========================================================================
    // 버전 비교
    // ==========================================================================

    /**
     * 두 버전 비교.
     */
    @Transactional(readOnly = true)
    public VersionComparisonResponse compareVersions(UUID templateId, Integer version1, Integer version2) {
        ApprovalLineTemplate template = findTemplateOrThrow(templateId);

        List<ApprovalLineTemplateVersion> versions = versionRepository
                .findVersionsForComparison(templateId, version1, version2);

        if (versions.size() != 2) {
            throw new ApprovalLineTemplateNotFoundException(
                    "비교할 버전을 찾을 수 없습니다: " + version1 + ", " + version2);
        }

        ApprovalLineTemplateVersion v1 = versions.get(0);
        ApprovalLineTemplateVersion v2 = versions.get(1);

        // 버전 순서 정렬 (낮은 버전이 먼저)
        if (v1.getVersion() > v2.getVersion()) {
            ApprovalLineTemplateVersion temp = v1;
            v1 = v2;
            v2 = temp;
        }

        return buildComparisonResponse(template, v1, v2);
    }

    private VersionComparisonResponse buildComparisonResponse(ApprovalLineTemplate template,
                                                              ApprovalLineTemplateVersion v1,
                                                              ApprovalLineTemplateVersion v2) {
        VersionSummary summary1 = new VersionSummary(
                v1.getVersion(),
                v1.getChangedBy(),
                v1.getChangedByName(),
                v1.getChangedAt().toString(),
                v1.getChangeAction().name(),
                v1.getChangeReason()
        );

        VersionSummary summary2 = new VersionSummary(
                v2.getVersion(),
                v2.getChangedBy(),
                v2.getChangedByName(),
                v2.getChangedAt().toString(),
                v2.getChangeAction().name(),
                v2.getChangeReason()
        );

        List<FieldDiff> commonFieldDiffs = compareFields(v1, v2);
        List<StepDiff> stepDiffs = compareSteps(v1, v2);

        // 공통 FieldDiff를 Response용 FieldDiff로 변환
        List<VersionComparisonResponse.FieldDiff> fieldDiffs = commonFieldDiffs.stream()
                .map(fd -> new VersionComparisonResponse.FieldDiff(
                        fd.fieldName(),
                        fd.fieldLabel(),
                        fd.beforeValue(),
                        fd.afterValue(),
                        VersionComparisonResponse.DiffType.valueOf(fd.diffType().name())
                ))
                .toList();

        return new VersionComparisonResponse(
                template.getId(),
                template.getTemplateCode(),
                summary1,
                summary2,
                fieldDiffs,
                stepDiffs
        );
    }

    private List<FieldDiff> compareFields(ApprovalLineTemplateVersion v1, ApprovalLineTemplateVersion v2) {
        // ObjectDiffUtils를 사용한 필드 비교
        return ObjectDiffUtils.compareFields(
                new VersionSnapshot(v1.getName(), v1.getDisplayOrder(), v1.getDescription(), v1.isActive()),
                new VersionSnapshot(v2.getName(), v2.getDisplayOrder(), v2.getDescription(), v2.isActive()),
                FIELD_LABELS
        );
    }

    /**
     * 비교용 스냅샷 record.
     */
    private record VersionSnapshot(String name, Integer displayOrder, String description, boolean active) {}

    private List<StepDiff> compareSteps(ApprovalLineTemplateVersion v1, ApprovalLineTemplateVersion v2) {
        List<StepDiff> diffs = new ArrayList<>();

        List<ApprovalTemplateStepVersion> steps1 = v1.getSteps();
        List<ApprovalTemplateStepVersion> steps2 = v2.getSteps();

        int maxSteps = Math.max(steps1.size(), steps2.size());

        for (int i = 0; i < maxSteps; i++) {
            ApprovalTemplateStepVersion step1 = i < steps1.size() ? steps1.get(i) : null;
            ApprovalTemplateStepVersion step2 = i < steps2.size() ? steps2.get(i) : null;

            if (step1 == null && step2 != null) {
                // 추가됨
                diffs.add(new StepDiff(
                        step2.getStepOrder(),
                        step2.getApprovalGroupCode(),
                        null,
                        step2.getApprovalGroupName(),
                        VersionComparisonResponse.DiffType.ADDED
                ));
            } else if (step1 != null && step2 == null) {
                // 삭제됨
                diffs.add(new StepDiff(
                        step1.getStepOrder(),
                        step1.getApprovalGroupCode(),
                        step1.getApprovalGroupName(),
                        null,
                        VersionComparisonResponse.DiffType.REMOVED
                ));
            } else if (step1 != null && step2 != null) {
                if (!Objects.equals(step1.getApprovalGroupCode(), step2.getApprovalGroupCode())) {
                    // 수정됨
                    diffs.add(new StepDiff(
                            step1.getStepOrder(),
                            step2.getApprovalGroupCode(),
                            step1.getApprovalGroupName(),
                            step2.getApprovalGroupName(),
                            VersionComparisonResponse.DiffType.MODIFIED
                    ));
                }
            }
        }

        return diffs;
    }

    // ==========================================================================
    // 버전 롤백 (이력화면 시나리오)
    // ==========================================================================

    /**
     * 특정 버전으로 롤백.
     * 이력화면에서 특정 버전을 선택하여 해당 시점의 상태로 복원합니다.
     */
    public VersionHistoryResponse rollbackToVersion(UUID templateId, RollbackRequest request, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 롤백 대상 버전 조회
        ApprovalLineTemplateVersion targetVersion = versionRepository
                .findByTemplateIdAndVersion(templateId, request.targetVersion())
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException(
                        "롤백할 버전을 찾을 수 없습니다: " + request.targetVersion()));

        // 현재 버전 종료
        ApprovalLineTemplateVersion currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성 (롤백)
        int nextVersionNumber = versionRepository.findMaxVersionByTemplateId(templateId) + 1;
        ApprovalLineTemplateVersion rollbackVersion = ApprovalLineTemplateVersion.createFromRollback(
                template,
                nextVersionNumber,
                targetVersion.getName(),
                targetVersion.getDisplayOrder(),
                targetVersion.getDescription(),
                targetVersion.isActive(),
                request.changeReason(),
                context.username(),
                context.username(),
                now,
                request.targetVersion()
        );

        // Steps 복사
        for (ApprovalTemplateStepVersion sourceStep : targetVersion.getSteps()) {
            ApprovalTemplateStepVersion newStep = ApprovalTemplateStepVersion.copyFrom(rollbackVersion, sourceStep);
            rollbackVersion.addStep(newStep);
        }

        rollbackVersion = versionRepository.save(rollbackVersion);

        // 메인 테이블 업데이트
        template.activateNewVersion(rollbackVersion, now);

        return VersionHistoryResponse.from(rollbackVersion);
    }

    // ==========================================================================
    // Draft/Published (목록화면 시나리오)
    // ==========================================================================

    /**
     * 초안 생성 또는 수정.
     * 목록화면에서 템플릿을 수정할 때 바로 적용하지 않고 초안으로 저장합니다.
     */
    public VersionHistoryResponse saveDraft(UUID templateId, DraftRequest request, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 기존 초안이 있는지 확인
        ApprovalLineTemplateVersion draft = versionRepository.findDraftByTemplateId(templateId)
                .orElse(null);

        if (draft != null) {
            // 기존 초안 수정
            draft.updateDraft(
                    request.name(),
                    request.displayOrder(),
                    request.description(),
                    request.active(),
                    request.changeReason(),
                    now
            );

            // Steps 교체
            draft.getSteps().clear();
            addStepsToDraft(draft, request.steps());

        } else {
            // 새 초안 생성
            int nextVersionNumber = versionRepository.findMaxVersionByTemplateId(templateId) + 1;
            draft = ApprovalLineTemplateVersion.createDraft(
                    template,
                    nextVersionNumber,
                    request.name(),
                    request.displayOrder(),
                    request.description(),
                    request.active(),
                    request.changeReason(),
                    context.username(),
                    context.username(),
                    now
            );

            addStepsToDraft(draft, request.steps());
            draft = versionRepository.save(draft);

            // 메인 테이블에 초안 링크 설정
            template.setDraftVersion(draft);
        }

        return VersionHistoryResponse.from(draft);
    }

    /**
     * 초안 조회.
     */
    @Transactional(readOnly = true)
    public VersionHistoryResponse getDraft(UUID templateId) {
        ApprovalLineTemplateVersion draft = versionRepository.findDraftByTemplateId(templateId)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("초안이 없습니다."));

        return VersionHistoryResponse.from(draft);
    }

    /**
     * 초안이 있는지 확인.
     */
    @Transactional(readOnly = true)
    public boolean hasDraft(UUID templateId) {
        return versionRepository.existsDraftByTemplateId(templateId);
    }

    /**
     * 초안 게시 (적용).
     * 초안을 현재 활성 버전으로 전환합니다.
     */
    public VersionHistoryResponse publishDraft(UUID templateId, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 초안 확인
        ApprovalLineTemplateVersion draft = versionRepository.findDraftByTemplateId(templateId)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("게시할 초안이 없습니다."));

        // 현재 버전 종료
        ApprovalLineTemplateVersion currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 초안 게시
        draft.publish(now);

        // 메인 테이블 업데이트
        template.activateNewVersion(draft, now);

        return VersionHistoryResponse.from(draft);
    }

    /**
     * 초안 삭제 (취소).
     */
    public void discardDraft(UUID templateId) {
        ApprovalLineTemplate template = findTemplateOrThrow(templateId);

        ApprovalLineTemplateVersion draft = versionRepository.findDraftByTemplateId(templateId)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("삭제할 초안이 없습니다."));

        // 메인 테이블에서 초안 링크 제거
        template.discardDraft();

        // 초안 삭제
        versionRepository.delete(draft);
    }

    // ==========================================================================
    // 버전 생성 헬퍼 메서드 (기존 서비스에서 호출)
    // ==========================================================================

    /**
     * 새 템플릿 생성 시 첫 번째 버전 생성.
     */
    public ApprovalLineTemplateVersion createInitialVersion(ApprovalLineTemplate template,
                                                            ApprovalLineTemplateRequest request,
                                                            AuthContext context,
                                                            OffsetDateTime now) {
        ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                template,
                1,
                request.name(),
                request.displayOrder(),
                request.description(),
                request.active(),
                ChangeAction.CREATE,
                null,
                context.username(),
                context.username(),
                now
        );

        addStepsToVersion(version, request.steps());
        version = versionRepository.save(version);

        template.activateNewVersion(version, now);

        return version;
    }

    /**
     * 템플릿 수정 시 새 버전 생성.
     */
    public ApprovalLineTemplateVersion createUpdateVersion(ApprovalLineTemplate template,
                                                           ApprovalLineTemplateRequest request,
                                                           AuthContext context,
                                                           OffsetDateTime now) {
        // 현재 버전 종료
        ApprovalLineTemplateVersion currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성
        int nextVersionNumber = versionRepository.findMaxVersionByTemplateId(template.getId()) + 1;
        ApprovalLineTemplateVersion newVersion = ApprovalLineTemplateVersion.create(
                template,
                nextVersionNumber,
                request.name(),
                request.displayOrder(),
                request.description(),
                request.active(),
                ChangeAction.UPDATE,
                null,
                context.username(),
                context.username(),
                now
        );

        addStepsToVersion(newVersion, request.steps());
        newVersion = versionRepository.save(newVersion);

        template.activateNewVersion(newVersion, now);

        return newVersion;
    }

    /**
     * 템플릿 삭제(비활성화) 시 새 버전 생성.
     */
    public ApprovalLineTemplateVersion createDeleteVersion(ApprovalLineTemplate template,
                                                           AuthContext context,
                                                           OffsetDateTime now) {
        ApprovalLineTemplateVersion currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByTemplateId(template.getId()) + 1;
        ApprovalLineTemplateVersion deleteVersion = ApprovalLineTemplateVersion.create(
                template,
                nextVersionNumber,
                template.getName(),
                template.getDisplayOrder(),
                template.getDescription(),
                false,  // 비활성화
                ChangeAction.DELETE,
                null,
                context.username(),
                context.username(),
                now
        );

        // 기존 Steps 복사
        if (currentVersion != null) {
            for (ApprovalTemplateStepVersion sourceStep : currentVersion.getSteps()) {
                ApprovalTemplateStepVersion newStep = ApprovalTemplateStepVersion.copyFrom(deleteVersion, sourceStep);
                deleteVersion.addStep(newStep);
            }
        }

        deleteVersion = versionRepository.save(deleteVersion);
        template.activateNewVersion(deleteVersion, now);

        return deleteVersion;
    }

    /**
     * 템플릿 활성화(복원) 시 새 버전 생성.
     */
    public ApprovalLineTemplateVersion createRestoreVersion(ApprovalLineTemplate template,
                                                            AuthContext context,
                                                            OffsetDateTime now) {
        ApprovalLineTemplateVersion currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByTemplateId(template.getId()) + 1;
        ApprovalLineTemplateVersion restoreVersion = ApprovalLineTemplateVersion.create(
                template,
                nextVersionNumber,
                template.getName(),
                template.getDisplayOrder(),
                template.getDescription(),
                true,  // 활성화
                ChangeAction.RESTORE,
                null,
                context.username(),
                context.username(),
                now
        );

        // 기존 Steps 복사
        if (currentVersion != null) {
            for (ApprovalTemplateStepVersion sourceStep : currentVersion.getSteps()) {
                ApprovalTemplateStepVersion newStep = ApprovalTemplateStepVersion.copyFrom(restoreVersion, sourceStep);
                restoreVersion.addStep(newStep);
            }
        }

        restoreVersion = versionRepository.save(restoreVersion);
        template.activateNewVersion(restoreVersion, now);

        return restoreVersion;
    }

    /**
     * 템플릿 복사 시 새 버전 생성.
     */
    public ApprovalLineTemplateVersion createCopyVersion(ApprovalLineTemplate newTemplate,
                                                         ApprovalLineTemplate sourceTemplate,
                                                         String newName,
                                                         String newDescription,
                                                         AuthContext context,
                                                         OffsetDateTime now) {
        ApprovalLineTemplateVersion copyVersion = ApprovalLineTemplateVersion.createFromCopy(
                newTemplate,
                1,
                newName,
                sourceTemplate.getDisplayOrder(),
                newDescription != null ? newDescription : sourceTemplate.getDescription(),
                true,
                context.username(),
                context.username(),
                now,
                sourceTemplate.getId()
        );

        // 원본의 현재 버전에서 Steps 복사
        ApprovalLineTemplateVersion sourceVersion = sourceTemplate.getCurrentVersion();
        if (sourceVersion != null) {
            for (ApprovalTemplateStepVersion sourceStep : sourceVersion.getSteps()) {
                ApprovalTemplateStepVersion newStep = ApprovalTemplateStepVersion.copyFrom(copyVersion, sourceStep);
                copyVersion.addStep(newStep);
            }
        }

        copyVersion = versionRepository.save(copyVersion);
        newTemplate.activateNewVersion(copyVersion, now);

        return copyVersion;
    }

    // ==========================================================================
    // 헬퍼 메서드
    // ==========================================================================

    private ApprovalLineTemplate findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("승인선 템플릿을 찾을 수 없습니다."));
    }

    private void addStepsToVersion(ApprovalLineTemplateVersion version, List<ApprovalTemplateStepRequest> stepRequests) {
        if (stepRequests == null) {
            return;
        }
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));

            ApprovalTemplateStepVersion step = ApprovalTemplateStepVersion.create(
                    version, stepRequest.stepOrder(), group);
            version.addStep(step);
        }
    }

    private void addStepsToDraft(ApprovalLineTemplateVersion draft, List<ApprovalTemplateStepRequest> stepRequests) {
        if (stepRequests == null) {
            return;
        }
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));

            ApprovalTemplateStepVersion step = ApprovalTemplateStepVersion.create(
                    draft, stepRequest.stepOrder(), group);
            draft.addStep(step);
        }
    }
}
