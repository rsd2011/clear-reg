package com.example.admin.permission.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.dto.PermissionGroupHistoryResponse;
import com.example.admin.permission.dto.PermissionGroupRootRequest;
import com.example.admin.permission.dto.PermissionGroupRootResponse;
import com.example.admin.permission.exception.PermissionGroupNotFoundException;
import com.example.admin.permission.repository.PermissionGroupRootRepository;

/**
 * 권한 그룹 관리 서비스.
 * <p>
 * 모든 변경은 {@link PermissionGroupVersioningService}를 통해 SCD Type 2 버전으로 기록됩니다.
 * PermissionGroupRoot는 버전 컨테이너 역할만 하며, 모든 비즈니스 데이터는
 * PermissionGroup (currentVersion)에 저장됩니다.
 * </p>
 */
@Service
@Transactional
public class PermissionGroupRootService {

    private final PermissionGroupRootRepository rootRepository;
    private final PermissionGroupVersioningService versionService;
    private final PermissionGroupService permissionGroupService;

    public PermissionGroupRootService(PermissionGroupRootRepository rootRepository,
                                       PermissionGroupVersioningService versionService,
                                       PermissionGroupService permissionGroupService) {
        this.rootRepository = rootRepository;
        this.versionService = versionService;
        this.permissionGroupService = permissionGroupService;
    }

    /**
     * 권한 그룹 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<PermissionGroupRootResponse> list(String keyword, boolean activeOnly) {
        return rootRepository.findAll().stream()
                .filter(p -> p.getCurrentVersion() != null)  // 버전이 있는 것만 조회
                .filter(p -> !activeOnly || p.isActive())
                .filter(p -> matchesKeyword(p, keyword))
                .sorted((a, b) -> nullSafeCompare(a.getName(), b.getName()))
                .map(PermissionGroupRootResponse::from)
                .toList();
    }

    /**
     * 권한 그룹 목록 조회 (페이징).
     */
    @Transactional(readOnly = true)
    public Page<PermissionGroupRootResponse> list(Pageable pageable) {
        return rootRepository.findAll(pageable)
                .map(PermissionGroupRootResponse::from);
    }

    /**
     * 권한 그룹 단일 조회.
     */
    @Transactional(readOnly = true)
    public PermissionGroupRootResponse getById(UUID id) {
        PermissionGroupRoot group = findGroupOrThrow(id);
        return PermissionGroupRootResponse.from(group);
    }

    /**
     * 그룹 코드로 조회.
     */
    @Transactional(readOnly = true)
    public PermissionGroupRootResponse getByGroupCode(String groupCode) {
        PermissionGroupRoot group = rootRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new PermissionGroupNotFoundException("권한 그룹을 찾을 수 없습니다: " + groupCode));
        return PermissionGroupRootResponse.from(group);
    }

    /**
     * 권한 그룹 생성.
     */
    public PermissionGroupRootResponse create(PermissionGroupRootRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        PermissionGroupRoot group = PermissionGroupRoot.create(now);
        rootRepository.save(group);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(group, request, context, now);

        return PermissionGroupRootResponse.from(group);
    }

    /**
     * 그룹 코드를 지정하여 권한 그룹 생성.
     */
    public PermissionGroupRootResponse createWithCode(String groupCode, PermissionGroupRootRequest request, AuthContext context) {
        if (rootRepository.existsByGroupCode(groupCode)) {
            throw new IllegalArgumentException("이미 존재하는 그룹 코드입니다: " + groupCode);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        PermissionGroupRoot group = PermissionGroupRoot.createWithCode(groupCode, now);
        rootRepository.save(group);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(group, request, context, now);

        return PermissionGroupRootResponse.from(group);
    }

    /**
     * 권한 그룹 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        PermissionGroupRoot group = findGroupOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 삭제 버전 생성
        versionService.createDeleteVersion(group, context, now);
    }

    /**
     * 권한 그룹 활성화 (복원).
     */
    public PermissionGroupRootResponse activate(UUID id, AuthContext context) {
        PermissionGroupRoot group = findGroupOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 복원 버전 생성
        versionService.createRestoreVersion(group, context, now);

        return PermissionGroupRootResponse.from(group);
    }

    /**
     * 변경 이력 조회 (SCD Type 2 버전 이력).
     */
    @Transactional(readOnly = true)
    public List<PermissionGroupHistoryResponse> getHistory(UUID groupId) {
        return versionService.getVersionHistory(groupId);
    }

    /**
     * 초안이 있는 권한 그룹 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<PermissionGroupRootResponse> listWithDraft() {
        return rootRepository.findAll().stream()
                .filter(PermissionGroupRoot::hasDraft)
                .map(PermissionGroupRootResponse::from)
                .toList();
    }

    private PermissionGroupRoot findGroupOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new PermissionGroupNotFoundException("권한 그룹을 찾을 수 없습니다."));
    }

    private boolean matchesKeyword(PermissionGroupRoot group, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return containsIgnoreCase(group.getName(), lowerKeyword)
                || containsIgnoreCase(group.getGroupCode(), lowerKeyword)
                || containsIgnoreCase(group.getDescription(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String target, String keyword) {
        if (target == null) {
            return false;
        }
        return target.toLowerCase().contains(keyword);
    }

    private int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareToIgnoreCase(b);
    }
}
