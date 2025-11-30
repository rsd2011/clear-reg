package com.example.admin.rowaccesspolicy.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyHistoryResponse;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootResponse;
import com.example.admin.rowaccesspolicy.exception.RowAccessPolicyRootNotFoundException;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRootRepository;
import com.example.common.security.RowScope;

/**
 * 행 접근 정책 관리 서비스.
 * <p>
 * 모든 변경은 {@link RowAccessPolicyVersioningService}를 통해 SCD Type 2 버전으로 기록됩니다.
 * RowAccessPolicyRoot는 버전 컨테이너 역할만 하며, 모든 비즈니스 데이터는
 * RowAccessPolicy (currentVersion)에 저장됩니다.
 * </p>
 */
@Service
@Transactional
public class RowAccessPolicyRootService {

    private final RowAccessPolicyRootRepository rootRepository;
    private final RowAccessPolicyVersioningService versionService;

    public RowAccessPolicyRootService(RowAccessPolicyRootRepository rootRepository,
                                      RowAccessPolicyVersioningService versionService) {
        this.rootRepository = rootRepository;
        this.versionService = versionService;
    }

    /**
     * 행 접근 정책 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyRootResponse> list(String keyword, boolean activeOnly) {
        return rootRepository.findAll().stream()
                .filter(p -> p.getCurrentVersion() != null)  // 버전이 있는 것만 조회
                .filter(p -> !activeOnly || p.isActive())
                .filter(p -> matchesKeyword(p, keyword))
                .sorted((a, b) -> {
                    Integer priorityA = a.getPriority();
                    Integer priorityB = b.getPriority();
                    if (priorityA == null && priorityB == null) return 0;
                    if (priorityA == null) return 1;
                    if (priorityB == null) return -1;
                    int cmp = Integer.compare(priorityA, priorityB);
                    return cmp != 0 ? cmp : nullSafeCompare(a.getName(), b.getName());
                })
                .map(RowAccessPolicyRootResponse::from)
                .toList();
    }

    /**
     * 행 접근 정책 목록 조회 (페이징).
     */
    @Transactional(readOnly = true)
    public Page<RowAccessPolicyRootResponse> list(Pageable pageable) {
        return rootRepository.findAllWithCurrentVersion(pageable)
                .map(RowAccessPolicyRootResponse::from);
    }

    /**
     * 활성화된 행 접근 정책 목록 조회 (우선순위순).
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyRootResponse> listActive() {
        return rootRepository.findAllActiveOrderByPriority().stream()
                .map(RowAccessPolicyRootResponse::from)
                .toList();
    }

    /**
     * 특정 FeatureCode의 활성 정책 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyRootResponse> listByFeatureCode(FeatureCode featureCode) {
        return rootRepository.findActiveByFeatureCode(featureCode).stream()
                .map(RowAccessPolicyRootResponse::from)
                .toList();
    }

    /**
     * 특정 RowScope의 활성 정책 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyRootResponse> listByRowScope(RowScope rowScope) {
        return rootRepository.findActiveByRowScope(rowScope).stream()
                .map(RowAccessPolicyRootResponse::from)
                .toList();
    }

    /**
     * 행 접근 정책 단일 조회.
     */
    @Transactional(readOnly = true)
    public RowAccessPolicyRootResponse getById(UUID id) {
        RowAccessPolicyRoot policy = findPolicyOrThrow(id);
        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 정책 코드로 조회.
     */
    @Transactional(readOnly = true)
    public RowAccessPolicyRootResponse getByPolicyCode(String policyCode) {
        RowAccessPolicyRoot policy = rootRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("행 접근 정책을 찾을 수 없습니다: " + policyCode));
        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 행 접근 정책 생성.
     */
    public RowAccessPolicyRootResponse create(RowAccessPolicyRootRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        RowAccessPolicyRoot policy = RowAccessPolicyRoot.create(now);
        rootRepository.save(policy);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(policy, request, context, now);

        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 정책 코드를 지정하여 행 접근 정책 생성.
     */
    public RowAccessPolicyRootResponse createWithCode(String policyCode, RowAccessPolicyRootRequest request, AuthContext context) {
        if (rootRepository.existsByPolicyCode(policyCode)) {
            throw new IllegalArgumentException("이미 존재하는 정책 코드입니다: " + policyCode);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        RowAccessPolicyRoot policy = RowAccessPolicyRoot.createWithCode(policyCode, now);
        rootRepository.save(policy);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(policy, request, context, now);

        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 행 접근 정책 수정.
     */
    public RowAccessPolicyRootResponse update(UUID id, RowAccessPolicyRootRequest request, AuthContext context) {
        RowAccessPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 새 버전 생성
        versionService.createUpdateVersion(policy, request, context, now);

        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 행 접근 정책 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        RowAccessPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 삭제 버전 생성
        versionService.createDeleteVersion(policy, context, now);
    }

    /**
     * 행 접근 정책 활성화 (복원).
     */
    public RowAccessPolicyRootResponse activate(UUID id, AuthContext context) {
        RowAccessPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 복원 버전 생성
        versionService.createRestoreVersion(policy, context, now);

        return RowAccessPolicyRootResponse.from(policy);
    }

    /**
     * 변경 이력 조회 (SCD Type 2 버전 이력).
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyHistoryResponse> getHistory(UUID policyId) {
        return versionService.getVersionHistory(policyId);
    }

    /**
     * 초안이 있는 정책 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<RowAccessPolicyRootResponse> listWithDraft() {
        return rootRepository.findAllWithDraft().stream()
                .map(RowAccessPolicyRootResponse::from)
                .toList();
    }

    private RowAccessPolicyRoot findPolicyOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new RowAccessPolicyRootNotFoundException("행 접근 정책을 찾을 수 없습니다."));
    }

    private boolean matchesKeyword(RowAccessPolicyRoot policy, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return containsIgnoreCase(policy.getName(), lowerKeyword)
                || containsIgnoreCase(policy.getPolicyCode(), lowerKeyword)
                || containsIgnoreCase(policy.getDescription(), lowerKeyword);
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
