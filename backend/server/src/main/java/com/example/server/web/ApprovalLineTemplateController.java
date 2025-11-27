package com.example.server.web;

import java.util.List;
import java.util.UUID;

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

import com.example.admin.approval.ApprovalLineTemplateAdminService;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.TemplateHistoryResponse;
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

    private final ApprovalLineTemplateAdminService templateService;

    public ApprovalLineTemplateController(ApprovalLineTemplateAdminService templateService) {
        this.templateService = templateService;
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
    public List<TemplateHistoryResponse> getTemplateHistory(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return templateService.getHistory(id).stream()
                .map(r -> TemplateHistoryResponse.apply(r, masker))
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

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
