package com.example.admin.maskingpolicy.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.maskingpolicy.domain.MaskingPolicyRoot;
import com.example.admin.maskingpolicy.dto.MaskingPolicyRootRequest;
import com.example.admin.maskingpolicy.dto.MaskingPolicyRootResponse;
import com.example.admin.maskingpolicy.dto.MaskingPolicyVersionHistoryResponse;
import com.example.admin.maskingpolicy.exception.MaskingPolicyRootNotFoundException;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRootRepository;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.domain.FeatureCode;

/**
 * 마스킹 정책 관리 서비스.
 * <p>
 * 모든 변경은 {@link MaskingPolicyVersionService}를 통해 SCD Type 2 버전으로 기록됩니다.
 * MaskingPolicyRoot는 버전 컨테이너 역할만 하며, 모든 비즈니스 데이터는
 * MaskingPolicyVersion (currentVersion)에 저장됩니다.
 * </p>
 */
@Service
@Transactional
public class MaskingPolicyRootService {

    private final MaskingPolicyRootRepository rootRepository;
    private final MaskingPolicyVersionService versionService;

    public MaskingPolicyRootService(MaskingPolicyRootRepository rootRepository,
                                     MaskingPolicyVersionService versionService) {
        this.rootRepository = rootRepository;
        this.versionService = versionService;
    }

    /**
     * 마스킹 정책 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<MaskingPolicyRootResponse> list(String keyword, boolean activeOnly) {
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
                .map(MaskingPolicyRootResponse::from)
                .toList();
    }

    /**
     * 마스킹 정책 목록 조회 (페이징).
     */
    @Transactional(readOnly = true)
    public Page<MaskingPolicyRootResponse> list(Pageable pageable) {
        return rootRepository.findAllWithCurrentVersion(pageable)
                .map(MaskingPolicyRootResponse::from);
    }

    /**
     * 활성화된 마스킹 정책 목록 조회 (우선순위순).
     */
    @Transactional(readOnly = true)
    public List<MaskingPolicyRootResponse> listActive() {
        return rootRepository.findAllActiveOrderByPriority().stream()
                .map(MaskingPolicyRootResponse::from)
                .toList();
    }

    /**
     * 특정 FeatureCode의 활성 정책 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<MaskingPolicyRootResponse> listByFeatureCode(FeatureCode featureCode) {
        return rootRepository.findActiveByFeatureCode(featureCode).stream()
                .map(MaskingPolicyRootResponse::from)
                .toList();
    }

    /**
     * 마스킹 정책 단일 조회.
     */
    @Transactional(readOnly = true)
    public MaskingPolicyRootResponse getById(UUID id) {
        MaskingPolicyRoot policy = findPolicyOrThrow(id);
        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 정책 코드로 조회.
     */
    @Transactional(readOnly = true)
    public MaskingPolicyRootResponse getByPolicyCode(String policyCode) {
        MaskingPolicyRoot policy = rootRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("마스킹 정책을 찾을 수 없습니다: " + policyCode));
        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 마스킹 정책 생성.
     */
    public MaskingPolicyRootResponse create(MaskingPolicyRootRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        MaskingPolicyRoot policy = MaskingPolicyRoot.create(now);
        rootRepository.save(policy);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(policy, request, context, now);

        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 정책 코드를 지정하여 마스킹 정책 생성.
     */
    public MaskingPolicyRootResponse createWithCode(String policyCode, MaskingPolicyRootRequest request, AuthContext context) {
        if (rootRepository.existsByPolicyCode(policyCode)) {
            throw new IllegalArgumentException("이미 존재하는 정책 코드입니다: " + policyCode);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        MaskingPolicyRoot policy = MaskingPolicyRoot.createWithCode(policyCode, now);
        rootRepository.save(policy);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(policy, request, context, now);

        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 마스킹 정책 수정.
     */
    public MaskingPolicyRootResponse update(UUID id, MaskingPolicyRootRequest request, AuthContext context) {
        MaskingPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 새 버전 생성
        versionService.createUpdateVersion(policy, request, context, now);

        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 마스킹 정책 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        MaskingPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 삭제 버전 생성
        versionService.createDeleteVersion(policy, context, now);
    }

    /**
     * 마스킹 정책 활성화 (복원).
     */
    public MaskingPolicyRootResponse activate(UUID id, AuthContext context) {
        MaskingPolicyRoot policy = findPolicyOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 복원 버전 생성
        versionService.createRestoreVersion(policy, context, now);

        return MaskingPolicyRootResponse.from(policy);
    }

    /**
     * 변경 이력 조회 (SCD Type 2 버전 이력).
     */
    @Transactional(readOnly = true)
    public List<MaskingPolicyVersionHistoryResponse> getHistory(UUID policyId) {
        return versionService.getVersionHistory(policyId);
    }

    /**
     * 초안이 있는 정책 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<MaskingPolicyRootResponse> listWithDraft() {
        return rootRepository.findAllWithDraft().stream()
                .map(MaskingPolicyRootResponse::from)
                .toList();
    }

    private MaskingPolicyRoot findPolicyOrThrow(UUID id) {
        return rootRepository.findById(id)
                .orElseThrow(() -> new MaskingPolicyRootNotFoundException("마스킹 정책을 찾을 수 없습니다."));
    }

    private boolean matchesKeyword(MaskingPolicyRoot policy, String keyword) {
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
