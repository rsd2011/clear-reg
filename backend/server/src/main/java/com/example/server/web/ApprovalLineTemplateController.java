package com.example.server.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.example.admin.approval.ApprovalLineTemplateService;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.DraftRequest;
import com.example.admin.approval.dto.RollbackRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.VersionComparisonResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.admin.approval.version.ApprovalLineTemplateVersionService;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionDeniedException;
import com.example.admin.permission.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingFunctions;
import com.example.common.policy.DataPolicyContextHolder;

/**
 * 승인선 템플릿 관리 API.
 */
@RestController
@Validated
@RequestMapping("/api/approval-line-templates")
@Tag(name = "Approval Line Template Admin", description = "승인선 템플릿 관리 API")
public class ApprovalLineTemplateController {

    private final ApprovalLineTemplateService templateService;
    private final ApprovalLineTemplateVersionService versionService;

    public ApprovalLineTemplateController(ApprovalLineTemplateService templateService,
                                          ApprovalLineTemplateVersionService versionService) {
        this.templateService = templateService;
        this.versionService = versionService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "승인선 템플릿 목록 조회")
    public List<ApprovalLineTemplateResponse> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return templateService.list(keyword, activeOnly).stream()
                .map(r -> ApprovalLineTemplateResponse.apply(r, masker))
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "승인선 템플릿 단일 조회")
    public ApprovalLineTemplateResponse getTemplate(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalLineTemplateResponse.apply(templateService.getById(id), masker);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.CREATE)
    @Operation(summary = "승인선 템플릿 생성")
    public ApprovalLineTemplateResponse createTemplate(@Valid @RequestBody ApprovalLineTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalLineTemplateResponse.apply(templateService.create(request, context), masker);
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "승인선 템플릿 수정")
    public ApprovalLineTemplateResponse updateTemplate(@PathVariable UUID id,
                                                       @Valid @RequestBody ApprovalLineTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalLineTemplateResponse.apply(templateService.update(id, request, context), masker);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.DELETE)
    @Operation(summary = "승인선 템플릿 삭제 (비활성화)")
    public void deleteTemplate(@PathVariable UUID id) {
        AuthContext context = currentContext();
        templateService.delete(id, context);
    }

    @PostMapping("/{id}/activate")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "승인선 템플릿 활성화 (복원)")
    public ApprovalLineTemplateResponse activateTemplate(@PathVariable UUID id) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalLineTemplateResponse.apply(templateService.activate(id, context), masker);
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.CREATE)
    @Operation(summary = "승인선 템플릿 복사")
    public TemplateCopyResponse copyTemplate(@PathVariable UUID id,
                                             @Valid @RequestBody TemplateCopyRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return TemplateCopyResponse.apply(templateService.copy(id, request, context), masker);
    }

    @GetMapping("/{id}/history")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "승인선 템플릿 변경 이력 조회")
    public List<VersionHistoryResponse> getTemplateHistory(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return templateService.getHistory(id).stream()
                .map(r -> VersionHistoryResponse.apply(r, masker))
                .toList();
    }

    @PatchMapping("/display-orders")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "승인선 템플릿 표시순서 일괄 변경")
    public List<ApprovalLineTemplateResponse> updateDisplayOrders(@Valid @RequestBody DisplayOrderUpdateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return templateService.updateDisplayOrders(request, context).stream()
                .map(r -> ApprovalLineTemplateResponse.apply(r, masker))
                .toList();
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API
    // ==========================================================================

    @GetMapping("/{id}/versions")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "버전 이력 조회 (SCD Type 2)")
    public List<VersionHistoryResponse> getVersionHistory(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return versionService.getVersionHistory(id).stream()
                .map(r -> VersionHistoryResponse.apply(r, masker))
                .toList();
    }

    @GetMapping("/{id}/versions/{version}")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "특정 버전 상세 조회")
    public VersionHistoryResponse getVersion(@PathVariable UUID id, @PathVariable Integer version) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.getVersion(id, version), masker);
    }

    @GetMapping("/{id}/versions/as-of")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "특정 시점 버전 조회 (Point-in-Time Query)")
    public VersionHistoryResponse getVersionAsOf(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime asOf) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.getVersionAsOf(id, asOf), masker);
    }

    @GetMapping("/{id}/versions/compare")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "버전 비교")
    public VersionComparisonResponse compareVersions(
            @PathVariable UUID id,
            @RequestParam Integer version1,
            @RequestParam Integer version2) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionComparisonResponse.apply(versionService.compareVersions(id, version1, version2), masker);
    }

    // ==========================================================================
    // 버전 롤백 (이력화면 시나리오)
    // ==========================================================================

    @PostMapping("/{id}/versions/rollback")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "버전 롤백", description = "이력화면에서 특정 버전을 선택하여 해당 시점의 상태로 복원합니다.")
    public VersionHistoryResponse rollbackToVersion(
            @PathVariable UUID id,
            @Valid @RequestBody RollbackRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.rollbackToVersion(id, request, context), masker);
    }

    // ==========================================================================
    // Draft/Published (목록화면 시나리오)
    // ==========================================================================

    @GetMapping("/{id}/draft")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "초안 조회")
    public VersionHistoryResponse getDraft(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.getDraft(id), masker);
    }

    @GetMapping("/{id}/draft/exists")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.READ)
    @Operation(summary = "초안 존재 여부 확인")
    public boolean hasDraft(@PathVariable UUID id) {
        return versionService.hasDraft(id);
    }

    @PostMapping("/{id}/draft")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 저장", description = "목록화면에서 템플릿을 수정할 때 바로 적용하지 않고 초안으로 저장합니다.")
    public VersionHistoryResponse saveDraft(
            @PathVariable UUID id,
            @Valid @RequestBody DraftRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.saveDraft(id, request, context), masker);
    }

    @PostMapping("/{id}/draft/publish")
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 게시", description = "초안을 현재 활성 버전으로 전환합니다.")
    public VersionHistoryResponse publishDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return VersionHistoryResponse.apply(versionService.publishDraft(id, context), masker);
    }

    @DeleteMapping("/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.APPROVAL_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 삭제", description = "저장된 초안을 삭제합니다.")
    public void discardDraft(@PathVariable UUID id) {
        versionService.discardDraft(id);
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
