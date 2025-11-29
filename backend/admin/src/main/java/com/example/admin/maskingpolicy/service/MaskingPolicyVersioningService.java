package com.example.admin.maskingpolicy.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.masking.DataKind;
import com.example.admin.maskingpolicy.domain.MaskingPolicyRoot;
import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.maskingpolicy.dto.MaskingPolicyDraftRequest;
import com.example.admin.maskingpolicy.dto.MaskingPolicyRootRequest;
import com.example.admin.maskingpolicy.dto.MaskingPolicyHistoryResponse;
import com.example.admin.maskingpolicy.exception.MaskingPolicyRootNotFoundException;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRootRepository;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRepository;
import com.example.admin.permission.context.AuthContext;
import com.example.common.version.ChangeAction;

/**
 * 마스킹 정책 버전 관리 서비스 (SCD Type 2).
 */
@Service
@Transactional
public class MaskingPolicyVersioningService {

    private final MaskingPolicyRootRepository rootRepository;
    private final MaskingPolicyRepository versionRepository;

    public MaskingPolicyVersioningService(MaskingPolicyRootRepository rootRepository,
                                               MaskingPolicyRepository versionRepository) {
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
    public List<MaskingPolicyHistoryResponse> getVersionHistory(UUID policyId) {
        findRootOrThrow(policyId);

        return versionRepository.findHistoryByRootId(policyId).stream()
                .map(MaskingPolicyHistoryResponse::from)
                .toList();
    }

    /**
     * 특정 버전 상세 조회.
     */
    @Transactional(readOnly = true)
    public MaskingPolicyHistoryResponse getVersion(UUID policyId, Integer versionNumber) {
        MaskingPolicy version = versionRepository
                .findByRootIdAndVersion(policyId, versionNumber)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException(
                        "버전을 찾을 수 없습니다: " + versionNumber));

        return MaskingPolicyHistoryResponse.from(version);
    }

    /**
     * 특정 시점의 버전 조회 (Point-in-Time Query).
     */
    @Transactional(readOnly = true)
    public MaskingPolicyHistoryResponse getVersionAsOf(UUID policyId, OffsetDateTime asOf) {
        MaskingPolicy version = versionRepository
                .findByRootIdAsOf(policyId, asOf)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException(
                        "해당 시점에 유효한 버전이 없습니다: " + asOf));

        return MaskingPolicyHistoryResponse.from(version);
    }

    // ==========================================================================
    // 버전 롤백
    // ==========================================================================

    /**
     * 특정 버전으로 롤백.
     */
    public MaskingPolicyHistoryResponse rollbackToVersion(UUID policyId,
                                                                  Integer targetVersion,
                                                                  String changeReason,
                                                                  AuthContext context) {
        MaskingPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        // 롤백 대상 버전 조회
        MaskingPolicy targetVersionEntity = versionRepository
                .findByRootIdAndVersion(policyId, targetVersion)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException(
                        "롤백할 버전을 찾을 수 없습니다: " + targetVersion));

        // 현재 버전 종료
        MaskingPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성 (롤백)
        int nextVersionNumber = versionRepository.findMaxVersionByRootId(policyId) + 1;
        MaskingPolicy rollbackVersion = MaskingPolicy.createFromRollback(
                root,
                nextVersionNumber,
                targetVersionEntity.getName(),
                targetVersionEntity.getDescription(),
                targetVersionEntity.getFeatureCode(),
                targetVersionEntity.getActionCode(),
                targetVersionEntity.getPermGroupCode(),
                targetVersionEntity.getOrgGroupCode(),
                targetVersionEntity.getDataKinds(),
                targetVersionEntity.getMaskingEnabled(),
                targetVersionEntity.getAuditEnabled(),
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

        return MaskingPolicyHistoryResponse.from(rollbackVersion);
    }

    // ==========================================================================
    // Draft/Published
    // ==========================================================================

    /**
     * 초안 생성 또는 수정.
     */
    public MaskingPolicyHistoryResponse saveDraft(UUID policyId,
                                                          MaskingPolicyDraftRequest request,
                                                          AuthContext context) {
        MaskingPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        Set<DataKind> dataKinds = convertDataKinds(request.dataKinds());

        // 기존 초안이 있는지 확인
        MaskingPolicy draft = versionRepository.findDraftByRootId(policyId)
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
                    dataKinds,
                    request.maskingEnabled(),
                    request.auditEnabled(),
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
            draft = MaskingPolicy.createDraft(
                    root,
                    nextVersionNumber,
                    request.name(),
                    request.description(),
                    request.featureCode(),
                    request.actionCode(),
                    request.permGroupCode(),
                    request.orgGroupCode(),
                    dataKinds,
                    request.maskingEnabled(),
                    request.auditEnabled(),
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

        return MaskingPolicyHistoryResponse.from(draft);
    }

    /**
     * 초안 조회.
     */
    @Transactional(readOnly = true)
    public MaskingPolicyHistoryResponse getDraft(UUID policyId) {
        MaskingPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("초안이 없습니다."));

        return MaskingPolicyHistoryResponse.from(draft);
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
    public MaskingPolicyHistoryResponse publishDraft(UUID policyId, AuthContext context) {
        MaskingPolicyRoot root = findRootOrThrow(policyId);
        OffsetDateTime now = OffsetDateTime.now();

        MaskingPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("게시할 초안이 없습니다."));

        // 현재 버전 종료
        MaskingPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 초안 게시
        draft.publish(now);
        root.activateNewVersion(draft, now);

        return MaskingPolicyHistoryResponse.from(draft);
    }

    /**
     * 초안 삭제 (취소).
     */
    public void discardDraft(UUID policyId) {
        MaskingPolicyRoot root = findRootOrThrow(policyId);

        MaskingPolicy draft = versionRepository.findDraftByRootId(policyId)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("삭제할 초안이 없습니다."));

        root.discardDraft();
        versionRepository.delete(draft);
    }

    // ==========================================================================
    // 버전 생성 헬퍼 메서드
    // ==========================================================================

    /**
     * 새 정책 생성 시 첫 번째 버전 생성.
     */
    public MaskingPolicy createInitialVersion(MaskingPolicyRoot root,
                                                      MaskingPolicyRootRequest request,
                                                      AuthContext context,
                                                      OffsetDateTime now) {
        Set<DataKind> dataKinds = convertDataKinds(request.dataKinds());

        MaskingPolicy version = MaskingPolicy.create(
                root,
                1,
                request.name(),
                request.description(),
                request.featureCode(),
                request.actionCode(),
                request.permGroupCode(),
                request.orgGroupCode(),
                dataKinds,
                request.maskingEnabled(),
                request.auditEnabled(),
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
    public MaskingPolicy createUpdateVersion(MaskingPolicyRoot root,
                                                     MaskingPolicyRootRequest request,
                                                     AuthContext context,
                                                     OffsetDateTime now) {
        // 현재 버전 종료
        MaskingPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        Set<DataKind> dataKinds = convertDataKinds(request.dataKinds());

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        MaskingPolicy newVersion = MaskingPolicy.create(
                root,
                nextVersionNumber,
                request.name(),
                request.description(),
                request.featureCode(),
                request.actionCode(),
                request.permGroupCode(),
                request.orgGroupCode(),
                dataKinds,
                request.maskingEnabled(),
                request.auditEnabled(),
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
    public MaskingPolicy createDeleteVersion(MaskingPolicyRoot root,
                                                     AuthContext context,
                                                     OffsetDateTime now) {
        MaskingPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        MaskingPolicy deleteVersion = MaskingPolicy.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? currentVersion.getPermGroupCode() : null,
                currentVersion != null ? currentVersion.getOrgGroupCode() : null,
                currentVersion != null ? currentVersion.getDataKinds() : null,
                currentVersion != null ? currentVersion.getMaskingEnabled() : null,
                currentVersion != null ? currentVersion.getAuditEnabled() : null,
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
    public MaskingPolicy createRestoreVersion(MaskingPolicyRoot root,
                                                      AuthContext context,
                                                      OffsetDateTime now) {
        MaskingPolicy currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        MaskingPolicy restoreVersion = MaskingPolicy.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? currentVersion.getPermGroupCode() : null,
                currentVersion != null ? currentVersion.getOrgGroupCode() : null,
                currentVersion != null ? currentVersion.getDataKinds() : null,
                currentVersion != null ? currentVersion.getMaskingEnabled() : null,
                currentVersion != null ? currentVersion.getAuditEnabled() : null,
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

    private MaskingPolicyRoot findRootOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("마스킹 정책을 찾을 수 없습니다."));
    }

    private Set<DataKind> convertDataKinds(Set<String> dataKindStrings) {
        if (dataKindStrings == null || dataKindStrings.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return dataKindStrings.stream()
                .map(DataKind::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
