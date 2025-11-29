package com.example.admin.approval.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.admin.approval.repository.ApprovalTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.exception.ApprovalGroupNotFoundException;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.exception.ApprovalTemplateRootNotFoundException;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DraftRequest;
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
public class ApprovalTemplateService {

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "name", "이름",
            "displayOrder", "표시순서",
            "description", "설명",
            "active", "활성화"
    );

    private final ApprovalTemplateRootRepository templateRepository;
    private final ApprovalTemplateRepository versionRepository;
    private final ApprovalGroupRepository groupRepository;

    public ApprovalTemplateService(ApprovalTemplateRootRepository templateRepository,
                                              ApprovalTemplateRepository versionRepository,
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

        return versionRepository.findHistoryByRootId(templateId).stream()
                .map(VersionHistoryResponse::from)
                .toList();
    }

    /**
     * 특정 버전 상세 조회.
     */
    @Transactional(readOnly = true)
    public VersionHistoryResponse getVersion(UUID templateId, Integer versionNumber) {
        ApprovalTemplate version = versionRepository
                .findByRootIdAndVersion(templateId, versionNumber)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException(
                        "버전을 찾을 수 없습니다: " + versionNumber));

        return VersionHistoryResponse.from(version);
    }

    /**
     * 특정 시점의 버전 조회 (Point-in-Time Query).
     */
    @Transactional(readOnly = true)
    public VersionHistoryResponse getVersionAsOf(UUID templateId, OffsetDateTime asOf) {
        ApprovalTemplate version = versionRepository
                .findByRootIdAsOf(templateId, asOf)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException(
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
        ApprovalTemplateRoot template = findTemplateOrThrow(templateId);

        List<ApprovalTemplate> versions = versionRepository
                .findVersionsForComparison(templateId, version1, version2);

        if (versions.size() != 2) {
            throw new ApprovalTemplateRootNotFoundException(
                    "비교할 버전을 찾을 수 없습니다: " + version1 + ", " + version2);
        }

        ApprovalTemplate v1 = versions.get(0);
        ApprovalTemplate v2 = versions.get(1);

        // 버전 순서 정렬 (낮은 버전이 먼저)
        if (v1.getVersion() > v2.getVersion()) {
            ApprovalTemplate temp = v1;
            v1 = v2;
            v2 = temp;
        }

        return buildComparisonResponse(template, v1, v2);
    }

    private VersionComparisonResponse buildComparisonResponse(ApprovalTemplateRoot template,
                                                              ApprovalTemplate v1,
                                                              ApprovalTemplate v2) {
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

    private List<FieldDiff> compareFields(ApprovalTemplate v1, ApprovalTemplate v2) {
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

    private List<StepDiff> compareSteps(ApprovalTemplate v1, ApprovalTemplate v2) {
        List<StepDiff> diffs = new ArrayList<>();

        List<ApprovalTemplateStep> steps1 = v1.getSteps();
        List<ApprovalTemplateStep> steps2 = v2.getSteps();

        int maxSteps = Math.max(steps1.size(), steps2.size());

        for (int i = 0; i < maxSteps; i++) {
            ApprovalTemplateStep step1 = i < steps1.size() ? steps1.get(i) : null;
            ApprovalTemplateStep step2 = i < steps2.size() ? steps2.get(i) : null;

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
     *
     * @param templateId    템플릿 ID
     * @param targetVersion 롤백할 버전 번호
     * @param changeReason  변경 사유
     * @param context       인증 컨텍스트
     */
    public VersionHistoryResponse rollbackToVersion(UUID templateId,
                                                    Integer targetVersion,
                                                    String changeReason,
                                                    AuthContext context) {
        ApprovalTemplateRoot template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 롤백 대상 버전 조회
        ApprovalTemplate targetVersionEntity = versionRepository
                .findByRootIdAndVersion(templateId, targetVersion)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException(
                        "롤백할 버전을 찾을 수 없습니다: " + targetVersion));

        // 현재 버전 종료
        ApprovalTemplate currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성 (롤백)
        int nextVersionNumber = versionRepository.findMaxVersionByRootId(templateId) + 1;
        ApprovalTemplate rollbackVersion = ApprovalTemplate.createFromRollback(
                template,
                nextVersionNumber,
                targetVersionEntity.getName(),
                targetVersionEntity.getDisplayOrder(),
                targetVersionEntity.getDescription(),
                targetVersionEntity.isActive(),
                changeReason,
                context.username(),
                context.username(),
                now,
                targetVersion
        );

        // Steps 복사
        for (ApprovalTemplateStep sourceStep : targetVersionEntity.getSteps()) {
            ApprovalTemplateStep newStep = ApprovalTemplateStep.copyFrom(rollbackVersion, sourceStep);
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
        ApprovalTemplateRoot template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 기존 초안이 있는지 확인
        ApprovalTemplate draft = versionRepository.findDraftByRootId(templateId)
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
            int nextVersionNumber = versionRepository.findMaxVersionByRootId(templateId) + 1;
            draft = ApprovalTemplate.createDraft(
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
        ApprovalTemplate draft = versionRepository.findDraftByRootId(templateId)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException("초안이 없습니다."));

        return VersionHistoryResponse.from(draft);
    }

    /**
     * 초안이 있는지 확인.
     */
    @Transactional(readOnly = true)
    public boolean hasDraft(UUID templateId) {
        return versionRepository.existsDraftByRootId(templateId);
    }

    /**
     * 초안 게시 (적용).
     * 초안을 현재 활성 버전으로 전환합니다.
     */
    public VersionHistoryResponse publishDraft(UUID templateId, AuthContext context) {
        ApprovalTemplateRoot template = findTemplateOrThrow(templateId);
        OffsetDateTime now = OffsetDateTime.now();

        // 초안 확인
        ApprovalTemplate draft = versionRepository.findDraftByRootId(templateId)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException("게시할 초안이 없습니다."));

        // 현재 버전 종료
        ApprovalTemplate currentVersion = template.getCurrentVersion();
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
        ApprovalTemplateRoot template = findTemplateOrThrow(templateId);

        ApprovalTemplate draft = versionRepository.findDraftByRootId(templateId)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException("삭제할 초안이 없습니다."));

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
    public ApprovalTemplate createInitialVersion(ApprovalTemplateRoot template,
                                                            ApprovalTemplateRootRequest request,
                                                            AuthContext context,
                                                            OffsetDateTime now) {
        ApprovalTemplate version = ApprovalTemplate.create(
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
    public ApprovalTemplate createUpdateVersion(ApprovalTemplateRoot template,
                                                           ApprovalTemplateRootRequest request,
                                                           AuthContext context,
                                                           OffsetDateTime now) {
        // 현재 버전 종료
        ApprovalTemplate currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성
        int nextVersionNumber = versionRepository.findMaxVersionByRootId(template.getId()) + 1;
        ApprovalTemplate newVersion = ApprovalTemplate.create(
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
    public ApprovalTemplate createDeleteVersion(ApprovalTemplateRoot template,
                                                           AuthContext context,
                                                           OffsetDateTime now) {
        ApprovalTemplate currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(template.getId()) + 1;
        ApprovalTemplate deleteVersion = ApprovalTemplate.create(
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
            for (ApprovalTemplateStep sourceStep : currentVersion.getSteps()) {
                ApprovalTemplateStep newStep = ApprovalTemplateStep.copyFrom(deleteVersion, sourceStep);
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
    public ApprovalTemplate createRestoreVersion(ApprovalTemplateRoot template,
                                                            AuthContext context,
                                                            OffsetDateTime now) {
        ApprovalTemplate currentVersion = template.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(template.getId()) + 1;
        ApprovalTemplate restoreVersion = ApprovalTemplate.create(
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
            for (ApprovalTemplateStep sourceStep : currentVersion.getSteps()) {
                ApprovalTemplateStep newStep = ApprovalTemplateStep.copyFrom(restoreVersion, sourceStep);
                restoreVersion.addStep(newStep);
            }
        }

        restoreVersion = versionRepository.save(restoreVersion);
        template.activateNewVersion(restoreVersion, now);

        return restoreVersion;
    }

    // ==========================================================================
    // 헬퍼 메서드
    // ==========================================================================

    private ApprovalTemplateRoot findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException("승인선 템플릿을 찾을 수 없습니다."));
    }

    private void addStepsToVersion(ApprovalTemplate version, List<ApprovalTemplateStepRequest> stepRequests) {
        if (stepRequests == null) {
            return;
        }
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));

            ApprovalTemplateStep step = ApprovalTemplateStep.create(
                    version, stepRequest.stepOrder(), group, stepRequest.skippable());
            version.addStep(step);
        }
    }

    private void addStepsToDraft(ApprovalTemplate draft, List<ApprovalTemplateStepRequest> stepRequests) {
        if (stepRequests == null) {
            return;
        }
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));

            ApprovalTemplateStep step = ApprovalTemplateStep.create(
                    draft, stepRequest.stepOrder(), group, stepRequest.skippable());
            draft.addStep(step);
        }
    }
}
