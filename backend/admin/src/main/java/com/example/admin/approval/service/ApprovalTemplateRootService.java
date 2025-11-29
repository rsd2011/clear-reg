package com.example.admin.approval.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.exception.ApprovalTemplateRootNotFoundException;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.permission.context.AuthContext;

/**
 * 승인선 템플릿 관리 서비스 (v1 API용).
 * <p>
 * 모든 변경은 {@link ApprovalTemplateService}를 통해 SCD Type 2 버전으로 기록됩니다.
 * ApprovalTemplateRoot는 버전 컨테이너 역할만 하며, 모든 비즈니스 데이터는
 * ApprovalTemplate (currentVersion)에 저장됩니다.
 * </p>
 */
@Service
@Transactional
public class ApprovalTemplateRootService {

    private final ApprovalTemplateRootRepository templateRepository;
    private final ApprovalTemplateService versionService;

    public ApprovalTemplateRootService(ApprovalTemplateRootRepository templateRepository,
                                            ApprovalTemplateService versionService) {
        this.templateRepository = templateRepository;
        this.versionService = versionService;
    }

    /**
     * 승인선 템플릿 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<ApprovalTemplateRootResponse> list(String keyword, boolean activeOnly) {
        return templateRepository.findAll().stream()
                .filter(t -> t.getCurrentVersion() != null)  // 버전이 있는 것만 조회
                .filter(t -> !activeOnly || t.isActive())
                .filter(t -> matchesKeyword(t, keyword))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    return cmp != 0 ? cmp : nullSafeCompare(a.getName(), b.getName());
                })
                .map(ApprovalTemplateRootResponse::from)
                .toList();
    }

    /**
     * 승인선 템플릿 단일 조회.
     */
    @Transactional(readOnly = true)
    public ApprovalTemplateRootResponse getById(UUID id) {
        ApprovalTemplateRoot template = findTemplateOrThrow(id);
        return ApprovalTemplateRootResponse.from(template);
    }

    /**
     * 승인선 템플릿 생성.
     */
    public ApprovalTemplateRootResponse create(ApprovalTemplateRootRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        // Root 생성 (버전 컨테이너만)
        ApprovalTemplateRoot template = ApprovalTemplateRoot.create(now);
        templateRepository.save(template);

        // SCD Type 2 첫 버전 생성 (비즈니스 데이터 포함)
        versionService.createInitialVersion(template, request, context, now);

        return ApprovalTemplateRootResponse.from(template);
    }

    /**
     * 승인선 템플릿 수정.
     */
    public ApprovalTemplateRootResponse update(UUID id, ApprovalTemplateRootRequest request, AuthContext context) {
        ApprovalTemplateRoot template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 새 버전 생성
        versionService.createUpdateVersion(template, request, context, now);

        return ApprovalTemplateRootResponse.from(template);
    }

    /**
     * 승인선 템플릿 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        ApprovalTemplateRoot template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 삭제 버전 생성
        versionService.createDeleteVersion(template, context, now);
    }

    /**
     * 승인선 템플릿 활성화 (복원).
     */
    public ApprovalTemplateRootResponse activate(UUID id, AuthContext context) {
        ApprovalTemplateRoot template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // SCD Type 2 복원 버전 생성
        versionService.createRestoreVersion(template, context, now);

        return ApprovalTemplateRootResponse.from(template);
    }

    /**
     * 변경 이력 조회 (SCD Type 2 버전 이력).
     */
    @Transactional(readOnly = true)
    public List<VersionHistoryResponse> getHistory(UUID templateId) {
        return versionService.getVersionHistory(templateId);
    }

    /**
     * 표시순서 일괄 변경.
     */
    public List<ApprovalTemplateRootResponse> updateDisplayOrders(DisplayOrderUpdateRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        return request.items().stream()
                .map(item -> {
                    ApprovalTemplateRoot template = findTemplateOrThrow(item.id());
                    ApprovalTemplate currentVersion = template.getCurrentVersion();

                    if (currentVersion == null) {
                        throw new ApprovalTemplateRootNotFoundException("현재 버전이 없습니다.");
                    }

                    int previousDisplayOrder = currentVersion.getDisplayOrder();

                    // 순서가 변경된 경우에만 버전 생성
                    if (previousDisplayOrder != item.displayOrder()) {
                        ApprovalTemplateRootRequest updateRequest = new ApprovalTemplateRootRequest(
                                currentVersion.getName(),
                                item.displayOrder(),
                                currentVersion.getDescription(),
                                currentVersion.isActive(),
                                currentVersion.getSteps().stream()
                                        .map(s -> new com.example.admin.approval.dto.ApprovalTemplateStepRequest(
                                                s.getStepOrder(),
                                                s.getApprovalGroupCode(),
                                                s.isSkippable()))
                                        .toList()
                        );
                        versionService.createUpdateVersion(template, updateRequest, context, now);
                    }

                    return ApprovalTemplateRootResponse.from(template);
                })
                .toList();
    }

    private ApprovalTemplateRoot findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApprovalTemplateRootNotFoundException("승인선 템플릿을 찾을 수 없습니다."));
    }

    private boolean matchesKeyword(ApprovalTemplateRoot template, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return containsIgnoreCase(template.getName(), lowerKeyword)
                || containsIgnoreCase(template.getTemplateCode(), lowerKeyword)
                || containsIgnoreCase(template.getDescription(), lowerKeyword);
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
