package com.example.admin.permission.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.common.version.ChangeAction;

/**
 * 권한 그룹 버전 관리 서비스 (SCD Type 2).
 */
@Service
@Transactional
public class PermissionGroupVersioningService {

    private final PermissionGroupRootRepository rootRepository;
    private final PermissionGroupRepository versionRepository;
    private final PermissionGroupService permissionGroupService;

    public PermissionGroupVersioningService(PermissionGroupRootRepository rootRepository,
                                             PermissionGroupRepository versionRepository,
                                             PermissionGroupService permissionGroupService) {
        this.rootRepository = rootRepository;
        this.versionRepository = versionRepository;
        this.permissionGroupService = permissionGroupService;
    }

    // ==========================================================================
    // 버전 이력 조회
    // ==========================================================================

    /**
     * 버전 이력 목록 조회 (최신순, Draft 제외).
     */
    @Transactional(readOnly = true)
    public List<PermissionGroupHistoryResponse> getVersionHistory(UUID groupId) {
        findRootOrThrow(groupId);

        return versionRepository.findHistoryByRootId(groupId).stream()
                .map(PermissionGroupHistoryResponse::from)
                .toList();
    }

    /**
     * 특정 버전 상세 조회.
     */
    @Transactional(readOnly = true)
    public PermissionGroupHistoryResponse getVersion(UUID groupId, Integer versionNumber) {
        PermissionGroup version = versionRepository
                .findByRootIdAndVersion(groupId, versionNumber)
                .orElseThrow(() -> new PermissionGroupNotFoundException(
                        "버전을 찾을 수 없습니다: " + versionNumber));

        return PermissionGroupHistoryResponse.from(version);
    }

    /**
     * 특정 시점의 버전 조회 (Point-in-Time Query).
     */
    @Transactional(readOnly = true)
    public PermissionGroupHistoryResponse getVersionAsOf(UUID groupId, OffsetDateTime asOf) {
        PermissionGroup version = versionRepository
                .findByRootIdAsOf(groupId, asOf)
                .orElseThrow(() -> new PermissionGroupNotFoundException(
                        "해당 시점에 유효한 버전이 없습니다: " + asOf));

        return PermissionGroupHistoryResponse.from(version);
    }

    /**
     * 두 버전 비교.
     */
    @Transactional(readOnly = true)
    public PermissionGroupCompareResponse compareVersions(UUID groupId, Integer version1, Integer version2) {
        List<PermissionGroup> versions = versionRepository.findVersionsForComparison(groupId, version1, version2);

        if (versions.size() != 2) {
            throw new PermissionGroupNotFoundException("비교할 버전을 찾을 수 없습니다.");
        }

        PermissionGroup v1 = versions.get(0);
        PermissionGroup v2 = versions.get(1);

        return PermissionGroupCompareResponse.from(v1, v2);
    }

    // ==========================================================================
    // 버전 롤백
    // ==========================================================================

    /**
     * 특정 버전으로 롤백.
     */
    public PermissionGroupHistoryResponse rollbackToVersion(UUID groupId,
                                                             Integer targetVersion,
                                                             String changeReason,
                                                             AuthContext context) {
        PermissionGroupRoot root = findRootOrThrow(groupId);
        OffsetDateTime now = OffsetDateTime.now();

        // 롤백 대상 버전 조회
        PermissionGroup targetVersionEntity = versionRepository
                .findByRootIdAndVersion(groupId, targetVersion)
                .orElseThrow(() -> new PermissionGroupNotFoundException(
                        "롤백할 버전을 찾을 수 없습니다: " + targetVersion));

        // 현재 버전 종료
        PermissionGroup currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 새 버전 생성 (롤백)
        int nextVersionNumber = versionRepository.findMaxVersionByRootId(groupId) + 1;
        PermissionGroup rollbackVersion = PermissionGroup.createFromRollback(
                root,
                nextVersionNumber,
                targetVersionEntity.getName(),
                targetVersionEntity.getDescription(),
                targetVersionEntity.isActive(),
                targetVersionEntity.getAssignments(),
                targetVersionEntity.getApprovalGroupCodes(),
                changeReason,
                context.username(),
                context.username(),
                now,
                targetVersion
        );

        rollbackVersion = versionRepository.save(rollbackVersion);
        root.activateNewVersion(rollbackVersion, now);

        // 캐시 무효화
        permissionGroupService.evict(root.getGroupCode());

        return PermissionGroupHistoryResponse.from(rollbackVersion);
    }

    // ==========================================================================
    // Draft/Published
    // ==========================================================================

    /**
     * 초안 생성 또는 수정.
     */
    public PermissionGroupHistoryResponse saveDraft(UUID groupId,
                                                     PermissionGroupDraftRequest request,
                                                     AuthContext context) {
        PermissionGroupRoot root = findRootOrThrow(groupId);
        OffsetDateTime now = OffsetDateTime.now();

        List<PermissionAssignment> assignments = convertAssignments(request.assignments());

        // 기존 초안이 있는지 확인
        PermissionGroup draft = versionRepository.findDraftByRootId(groupId)
                .orElse(null);

        if (draft != null) {
            // 기존 초안 수정
            draft.updateDraft(
                    request.name(),
                    request.description(),
                    request.active(),
                    assignments,
                    request.approvalGroupCodes(),
                    request.changeReason(),
                    now
            );
        } else {
            // 새 초안 생성
            int nextVersionNumber = versionRepository.findMaxVersionByRootId(groupId) + 1;
            draft = PermissionGroup.createDraft(
                    root,
                    nextVersionNumber,
                    request.name(),
                    request.description(),
                    request.active(),
                    assignments,
                    request.approvalGroupCodes(),
                    request.changeReason(),
                    context.username(),
                    context.username(),
                    now
            );

            draft = versionRepository.save(draft);
            root.setDraftVersion(draft);
        }

        return PermissionGroupHistoryResponse.from(draft);
    }

    /**
     * 초안 조회.
     */
    @Transactional(readOnly = true)
    public PermissionGroupHistoryResponse getDraft(UUID groupId) {
        PermissionGroup draft = versionRepository.findDraftByRootId(groupId)
                .orElseThrow(() -> new PermissionGroupNotFoundException("초안이 없습니다."));

        return PermissionGroupHistoryResponse.from(draft);
    }

    /**
     * 초안이 있는지 확인.
     */
    @Transactional(readOnly = true)
    public boolean hasDraft(UUID groupId) {
        return versionRepository.existsDraftByRootId(groupId);
    }

    /**
     * 초안 게시 (적용).
     */
    public PermissionGroupHistoryResponse publishDraft(UUID groupId, AuthContext context) {
        PermissionGroupRoot root = findRootOrThrow(groupId);
        OffsetDateTime now = OffsetDateTime.now();

        PermissionGroup draft = versionRepository.findDraftByRootId(groupId)
                .orElseThrow(() -> new PermissionGroupNotFoundException("게시할 초안이 없습니다."));

        // 현재 버전 종료
        PermissionGroup currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        // 초안 게시
        draft.publish(now);
        root.activateNewVersion(draft, now);

        // 캐시 무효화
        permissionGroupService.evict(root.getGroupCode());

        return PermissionGroupHistoryResponse.from(draft);
    }

    /**
     * 초안 삭제 (취소).
     */
    public void discardDraft(UUID groupId) {
        PermissionGroupRoot root = findRootOrThrow(groupId);

        PermissionGroup draft = versionRepository.findDraftByRootId(groupId)
                .orElseThrow(() -> new PermissionGroupNotFoundException("삭제할 초안이 없습니다."));

        root.discardDraft();
        versionRepository.delete(draft);
    }

    // ==========================================================================
    // 버전 생성 헬퍼 메서드
    // ==========================================================================

    /**
     * 새 권한 그룹 생성 시 첫 번째 버전 생성.
     */
    public PermissionGroup createInitialVersion(PermissionGroupRoot root,
                                                 PermissionGroupRootRequest request,
                                                 AuthContext context,
                                                 OffsetDateTime now) {
        List<PermissionAssignment> assignments = convertAssignments(request.assignments());

        PermissionGroup version = PermissionGroup.create(
                root,
                1,
                request.name(),
                request.description(),
                request.active(),
                assignments,
                request.approvalGroupCodes(),
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
     * 권한 그룹 삭제(비활성화) 시 새 버전 생성.
     */
    public PermissionGroup createDeleteVersion(PermissionGroupRoot root,
                                                AuthContext context,
                                                OffsetDateTime now) {
        PermissionGroup currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        PermissionGroup deleteVersion = PermissionGroup.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                false,  // 비활성화
                currentVersion != null ? currentVersion.getAssignments() : List.of(),
                currentVersion != null ? currentVersion.getApprovalGroupCodes() : List.of(),
                ChangeAction.DELETE,
                null,
                context.username(),
                context.username(),
                now
        );

        deleteVersion = versionRepository.save(deleteVersion);
        root.activateNewVersion(deleteVersion, now);

        // 캐시 무효화
        permissionGroupService.evict(root.getGroupCode());

        return deleteVersion;
    }

    /**
     * 권한 그룹 활성화(복원) 시 새 버전 생성.
     */
    public PermissionGroup createRestoreVersion(PermissionGroupRoot root,
                                                 AuthContext context,
                                                 OffsetDateTime now) {
        PermissionGroup currentVersion = root.getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.close(now);
        }

        int nextVersionNumber = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
        PermissionGroup restoreVersion = PermissionGroup.create(
                root,
                nextVersionNumber,
                currentVersion != null ? currentVersion.getName() : null,
                currentVersion != null ? currentVersion.getDescription() : null,
                true,  // 활성화
                currentVersion != null ? currentVersion.getAssignments() : List.of(),
                currentVersion != null ? currentVersion.getApprovalGroupCodes() : List.of(),
                ChangeAction.RESTORE,
                null,
                context.username(),
                context.username(),
                now
        );

        restoreVersion = versionRepository.save(restoreVersion);
        root.activateNewVersion(restoreVersion, now);

        // 캐시 무효화
        permissionGroupService.evict(root.getGroupCode());

        return restoreVersion;
    }

    // ==========================================================================
    // 헬퍼 메서드
    // ==========================================================================

    private PermissionGroupRoot findRootOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new PermissionGroupNotFoundException("권한 그룹을 찾을 수 없습니다."));
    }

    private List<PermissionAssignment> convertAssignments(List<PermissionAssignmentDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream()
                .map(PermissionAssignmentDto::toEntity)
                .collect(Collectors.toList());
    }
}
