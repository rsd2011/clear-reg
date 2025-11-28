package com.example.admin.approval.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.admin.approval.exception.ApprovalLineTemplateNotFoundException;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.admin.approval.exception.ApprovalGroupNotFoundException;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.permission.context.AuthContext;

/**
 * 승인선 템플릿 관리 서비스 (v1 API용).
 * <p>
 * 모든 변경은 {@link ApprovalLineTemplateVersionService}를 통해 SCD Type 2 버전으로 기록됩니다.
 * </p>
 */
@Service
@Transactional
public class ApprovalLineTemplateService {

    private final ApprovalLineTemplateRepository templateRepository;
    private final ApprovalGroupRepository groupRepository;
    private final ApprovalLineTemplateVersionService versionService;

    public ApprovalLineTemplateService(ApprovalLineTemplateRepository templateRepository,
                                            ApprovalGroupRepository groupRepository,
                                            ApprovalLineTemplateVersionService versionService) {
        this.templateRepository = templateRepository;
        this.groupRepository = groupRepository;
        this.versionService = versionService;
    }

    /**
     * 승인선 템플릿 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<ApprovalLineTemplateResponse> list(String keyword, boolean activeOnly) {
        return templateRepository.findAll().stream()
                .filter(t -> !activeOnly || t.isActive())
                .filter(t -> matchesKeyword(t, keyword))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    return cmp != 0 ? cmp : a.getName().compareToIgnoreCase(b.getName());
                })
                .map(ApprovalLineTemplateResponse::from)
                .toList();
    }

    /**
     * 승인선 템플릿 단일 조회.
     */
    @Transactional(readOnly = true)
    public ApprovalLineTemplateResponse getById(UUID id) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 생성.
     */
    public ApprovalLineTemplateResponse create(ApprovalLineTemplateRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalLineTemplate template = ApprovalLineTemplate.create(
                request.name(),
                request.displayOrder(),
                request.description(),
                now);

        addStepsToTemplate(template, request.steps());
        templateRepository.save(template);

        // SCD Type 2 버전 생성
        versionService.createInitialVersion(template, request, context, now);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 수정.
     */
    public ApprovalLineTemplateResponse update(UUID id, ApprovalLineTemplateRequest request, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        template.rename(request.name(), request.displayOrder(), request.description(), request.active(), now);
        template.getSteps().clear();
        addStepsToTemplate(template, request.steps());

        // SCD Type 2 버전 생성
        versionService.createUpdateVersion(template, request, context, now);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        template.rename(template.getName(), template.getDisplayOrder(), template.getDescription(), false, now);

        // SCD Type 2 버전 생성
        versionService.createDeleteVersion(template, context, now);
    }

    /**
     * 승인선 템플릿 활성화 (복원).
     */
    public ApprovalLineTemplateResponse activate(UUID id, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        template.rename(template.getName(), template.getDisplayOrder(), template.getDescription(), true, now);

        // SCD Type 2 버전 생성
        versionService.createRestoreVersion(template, context, now);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 복사.
     */
    public TemplateCopyResponse copy(UUID sourceId, TemplateCopyRequest request, AuthContext context) {
        ApprovalLineTemplate source = findTemplateOrThrow(sourceId);
        OffsetDateTime now = OffsetDateTime.now();

        // 새 템플릿 생성
        ApprovalLineTemplate copied = ApprovalLineTemplate.create(
                request.name(),
                source.getDisplayOrder(),
                request.description() != null ? request.description() : source.getDescription(),
                now);

        // 원본의 steps 복제
        for (ApprovalTemplateStep step : source.getSteps()) {
            copied.addStep(step.getStepOrder(), step.getApprovalGroup());
        }

        templateRepository.save(copied);

        // SCD Type 2 버전 생성
        versionService.createCopyVersion(copied, source, request.name(), request.description(), context, now);

        return TemplateCopyResponse.from(copied, source);
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
    public List<ApprovalLineTemplateResponse> updateDisplayOrders(DisplayOrderUpdateRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        return request.items().stream()
                .map(item -> {
                    ApprovalLineTemplate template = findTemplateOrThrow(item.id());

                    int previousDisplayOrder = template.getDisplayOrder();
                    template.rename(template.getName(), item.displayOrder(), template.getDescription(), template.isActive(), now);

                    // 순서가 변경된 경우에만 버전 생성
                    if (previousDisplayOrder != item.displayOrder()) {
                        ApprovalLineTemplateRequest versionRequest = new ApprovalLineTemplateRequest(
                                template.getName(),
                                template.getDisplayOrder(),
                                template.getDescription(),
                                template.isActive(),
                                template.getSteps().stream()
                                        .map(s -> new ApprovalTemplateStepRequest(s.getStepOrder(), s.getApprovalGroup().getGroupCode()))
                                        .toList()
                        );
                        versionService.createUpdateVersion(template, versionRequest, context, now);
                    }

                    return ApprovalLineTemplateResponse.from(template);
                })
                .toList();
    }

    private ApprovalLineTemplate findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("승인선 템플릿을 찾을 수 없습니다."));
    }

    private void addStepsToTemplate(ApprovalLineTemplate template, List<ApprovalTemplateStepRequest> stepRequests) {
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));
            template.addStep(stepRequest.stepOrder(), group);
        }
    }

    private boolean matchesKeyword(ApprovalLineTemplate template, String keyword) {
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
}
