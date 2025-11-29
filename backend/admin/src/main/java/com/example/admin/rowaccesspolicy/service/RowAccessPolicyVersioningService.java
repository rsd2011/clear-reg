package com.example.admin.rowaccesspolicy.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyDraftRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyHistoryResponse;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootRequest;
import com.example.admin.rowaccesspolicy.exception.RowAccessPolicyRootNotFoundException;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRootRepository;
import com.example.common.version.ChangeAction;

/**
 * 행 접근 정책 버전 관리 서비스 (SCD Type 2).
 */
@Service
@Transactional
public class RowAccessPolicyVersioningService {

    private final RowAccessPolicyRootRepository rootRepository;
    private final RowAccessPolicyRepository versionRepository;

    public RowAccessPolicyVersioningService(RowAccessPolicyRootRepository rootRepository,
                                            RowAccessPolicyRepository versionRepository) {
        this.rootRepository = rootRepository;
        this.versionRepository = versionRepository;
    }

    // ==========================================================================
    // 버전 이력 조회
    // ==========================================================================

    /**
     * 버전 이력 목록 조회 (최신순, Draft 제외).
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyHistoryResponse> getVersionHistory(UUID policyId) {
        findRootOrThrow(policyId);

        return versionRepository.findHistoryByRootId(policyId).stream()
                .map(RowAccessPolicyHistoryResponse::from)
                .toList();
    }

    /**
     * 특정 버전 상세 조회.
     */
    @Transactional(readOnly = true)
    public RowAccessPolicyHistoryResponse getVersion(UUID policyId, Integer versionNumber) {
        RowAccessPolicy version = versionRepository
                .findByRootIdAndVersion(policyId, versionNumber)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException(
                        "버전을 찾을 수 없습니다: " + versionNumber));

        return RowAccessPolicyHistoryResponse.from(version);
    }

    /**
     * 특정 시점의 버전 조회 (Point-in-Time Query).
     */
    @Transactional(readOnly = true)
    public RowAccessPolicyHistoryResponse getVersionAsOf(UUID policyId, OffsetDateTime asOf) {
        RowAccessPolicy version = versionRepository
                .findByRootIdAsOf(policyId, asOf)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException(
                        "해당 시점에 유효한 버전이 없습니다: " + asOf));

        return RowAccessPolicyHistoryResponse.from(version);
    }

    // ==========================================================================
    // 버전 롤백
    // ==========================================================================

    /**
     * 특정 버전으로 롤백.
     */
    public RowAccessPolicyHistoryResponse rollbackToVersion(UUID policyId,
                                                            Integer targetVersion,
                                                            String changeReason,
                                                            AuthContext context) {
        RowAccessPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        // 롤백 대상 버전 조회
        RowAccessPolicy targetVersionEntity = versionRepository
                .findByRootIdAndVersion(policyId, targetVersion)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException(
                        "롤백할 버전을 찾을 수 없습니다: " + targetVersion));

        // 현재 버전 종료
        RowAccessPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성 (롤백)
        int nextVersionNumber = versionRepository.findMaxVersionByRootId(policyId) + 1;
        RowAccessPolicy rollbackVersion = RowAccessPolicy.createFromRollback(
                root,
                nextVersionNumber,
                targetVersionEntity.getName(),
                targetVersionEntity.getDescription(),
                targetVersionEntity.getFeatureCode(),
                targetVersionEntity.getActionCode(),
                targetVersionEntity.getPermGroupCode(),
                targetVersionEntity.getOrgGroupCode(),
                targetVersionEntity.getRowScope(),
                targetVersionEntity.getPriority(),
                targetVersionEntity.isActive(),
                targetVersionEntity.getEffectiveFrom(),
                targetVersionEntity.getEffectiveTo(),
                changeReason,
                context.username(),
                context.username(),
                now,
                targetVersion
        );

        rollbackVersion = versionRepository.save(rollbackVersion);
        root.activateNewVersion(rollbackVersion, now);

        return RowAccessPolicyHistoryResponse.from(rollbackVersion);
    }

    // ==========================================================================
    // Draft/Published
    // ==========================================================================

    /**
     * 초안 생성 또는 수정.
     */
    public RowAccessPolicyHistoryResponse saveDraft(UUID policyId,
                                                    RowAccessPolicyDraftRequest request,
                                                    AuthContext context) {
        RowAccessPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        // 기존 초안이 있는지 확인
        RowAccessPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElse(null);

        if (draft != null) {
            // 기존 초안 수정
            draft.updateDraft(
                    request.name(),
                    request.description(),
                    request.featureCode(),
                    request.actionCode(),
                    request.permGroupCode(),
                    request.orgGroupCode(),
                    request.rowScope(),
                    request.priority(),
                    request.active(),
                    request.effectiveFrom(),
                    request.effectiveTo(),
                    request.changeReason(),
                    now
            );
        } else {
            // 새 초안 생성
            int nextVersionNumber = versionRepository.findMaxVersionByRootId(policyId) + 1;
            draft = RowAccessPolicy.createDraft(
                    root,
                    nextVersionNumber,
                    request.name(),
                    request.description(),
                    request.featureCode(),
                    request.actionCode(),
                    request.permGroupCode(),
                    request.orgGroupCode(),
                    request.rowScope(),
                    request.priority(),
                    request.active(),
                    request.effectiveFrom(),
                    request.effectiveTo(),
                    request.changeReason(),
                    context.username(),
                    context.username(),
                    now
            );

            draft = versionRepository.save(draft);
            root.setDraftVersion(draft);
        }

        return RowAccessPolicyHistoryResponse.from(draft);
    }

    /**
     * 초안 조회.
     */
    @Transactional(readOnly = true)
    public RowAccessPolicyHistoryResponse getDraft(UUID policyId) {
        RowAccessPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("초안이 없습니다."));

        return RowAccessPolicyHistoryResponse.from(draft);
    }

    /**
     * 초안이 있는지 확인.
     */
    @Transactional(readOnly = true)
    public boolean hasDraft(UUID policyId) {
        return versionRepository.existsDraftByRootId(policyId);
    }

    /**
     * 초안 게시 (적용).
     */
    public RowAccessPolicyHistoryResponse publishDraft(UUID policyId, AuthContext context) {
        RowAccessPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        RowAccessPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("게시할 초안이 없습니다."));

        // 현재 버전 종료
        RowAccessPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 초안 게시
        draft.publish(now);
        root.activateNewVersion(draft, now);

        return RowAccessPolicyHistoryResponse.from(draft);
    }

    /**
     * 초안 삭제 (취소).
     */
    public void discardDraft(UUID policyId) {
        RowAccessPolicyRoot root = findRootOrThrow(policyId);

        RowAccessPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("삭제할 초안이 없습니다."));

        root.discardDraft();
        versionRepository.delete(draft);
    }

    // ==========================================================================
    // 버전 생성 헬퍼 메서드
    // ==========================================================================

    /**
     * 새 정책 생성 시 첫 번째 버전 생성.
     */
    public RowAccessPolicy createInitialVersion(RowAccessPolicyRoot root,
                                                RowAccessPolicyRootRequest request,
                                                AuthContext context,
                                                OffsetDateTime now) {
        RowAccessPolicy version = RowAccessPolicy.create(
                root,
                1,
                request.name(),
                request.description(),
                request.featureCode(),
                request.actionCode(),
                request.permGroupCode(),
                request.orgGroupCode(),
                request.rowScope(),
                request.priority(),
                request.active(),
                request.effectiveFrom(),
                request.effectiveTo(),
                ChangeAction.CREATE,
                null,
                context.username(),
                context.username(),
                now
        );

        version = versionRepository.save(version);
        root.activateNewVersion(version, now);

        return version;
    }

    /**
     * 정책 수정 시 새 버전 생성.
     */
    public RowAccessPolicy createUpdateVersion(RowAccessPolicyRoot root,
                                               RowAccessPolicyRootRequest request,
                                               AuthContext context,
                                               OffsetDateTime now) {
        // 현재 버전 종료
        RowAccessPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        RowAccessPolicy newVersion = RowAccessPolicy.create(
                root,
                nextVersionNumber,
                request.name(),
                request.description(),
                request.featureCode(),
                request.actionCode(),
                request.permGroupCode(),
                request.orgGroupCode(),
                request.rowScope(),
                request.priority(),
                request.active(),
                request.effectiveFrom(),
                request.effectiveTo(),
                ChangeAction.UPDATE,
                null,
                context.username(),
                context.username(),
                now
        );

        newVersion = versionRepository.save(newVersion);
        root.activateNewVersion(newVersion, now);

        return newVersion;
    }

    /**
     * 정책 삭제(비활성화) 시 새 버전 생성.
     */
    public RowAccessPolicy createDeleteVersion(RowAccessPolicyRoot root,
                                               AuthContext context,
                                               OffsetDateTime now) {
        RowAccessPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        RowAccessPolicy deleteVersion = RowAccessPolicy.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? currentVersion.getPermGroupCode() : null,
                currentVersion != null ? currentVersion.getOrgGroupCode() : null,
                currentVersion != null ? currentVersion.getRowScope() : null,
                currentVersion != null ? currentVersion.getPriority() : null,
                false,  // 비활성화
                currentVersion != null ? currentVersion.getEffectiveFrom() : null,
                currentVersion != null ? currentVersion.getEffectiveTo() : null,
                ChangeAction.DELETE,
                null,
                context.username(),
                context.username(),
                now
        );

        deleteVersion = versionRepository.save(deleteVersion);
        root.activateNewVersion(deleteVersion, now);

        return deleteVersion;
    }

    /**
     * 정책 활성화(복원) 시 새 버전 생성.
     */
    public RowAccessPolicy createRestoreVersion(RowAccessPolicyRoot root,
                                                AuthContext context,
                                                OffsetDateTime now) {
        RowAccessPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        RowAccessPolicy restoreVersion = RowAccessPolicy.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? currentVersion.getPermGroupCode() : null,
                currentVersion != null ? currentVersion.getOrgGroupCode() : null,
                currentVersion != null ? currentVersion.getRowScope() : null,
                currentVersion != null ? currentVersion.getPriority() : null,
                true,  // 활성화
                currentVersion != null ? currentVersion.getEffectiveFrom() : null,
                currentVersion != null ? currentVersion.getEffectiveTo() : null,
                ChangeAction.RESTORE,
                null,
                context.username(),
                context.username(),
                now
        );

        restoreVersion = versionRepository.save(restoreVersion);
        root.activateNewVersion(restoreVersion, now);

        return restoreVersion;
    }

    // ==========================================================================
    // 헬퍼 메서드
    // ==========================================================================

    private RowAccessPolicyRoot findRootOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("행 접근 정책을 찾을 수 없습니다."));
    }
}
