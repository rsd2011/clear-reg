package com.example.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.common.masking.MaskingFunctions;
import com.example.draft.application.TemplateAdminService;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftTemplatePresetRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.application.dto.DraftTemplatePresetResponse;

@RestController
@Validated
@RequestMapping("/api/draft-admin")
@Tag(name = "Draft Template Admin", description = "기안 폼/승인선/프리셋 관리 API")
public class DraftTemplateAdminController {

    private final TemplateAdminService templateAdminService;

    public DraftTemplateAdminController(TemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    // Approval Line Templates
    @PostMapping("/approval-line-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인선 템플릿 등록")
    public ApprovalTemplateRootResponse createApprovalTemplateRoot(@Valid @RequestBody ApprovalTemplateRootRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return ApprovalTemplateRootResponse.apply(templateAdminService.createApprovalTemplateRoot(request, context, true), masker);
    }

    @PutMapping("/approval-line-templates/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인선 템플릿 수정")
    public ApprovalTemplateRootResponse updateApprovalTemplateRoot(@PathVariable UUID id,
                                                                    @Valid @RequestBody ApprovalTemplateRootRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return ApprovalTemplateRootResponse.apply(templateAdminService.updateApprovalTemplateRoot(id, request, context, true), masker);
    }

    @GetMapping("/approval-line-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인선 템플릿 목록 조회")
    public List<ApprovalTemplateRootResponse> listApprovalTemplateRoots(@RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return templateAdminService.listApprovalTemplateRoots(null, null, activeOnly, context, true).stream()
                .map(t -> ApprovalTemplateRootResponse.apply(t, masker))
                .toList();
    }

    // Draft Form Templates
    @PostMapping("/form-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 등록")
    public DraftFormTemplateResponse createDraftFormTemplate(@Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(templateAdminService.createDraftFormTemplate(request, context, true), masker);
    }

    @PutMapping("/form-templates/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 수정")
    public DraftFormTemplateResponse updateDraftFormTemplate(@PathVariable UUID id,
                                                             @Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(templateAdminService.updateDraftFormTemplate(id, request, context, true), masker);
    }

    @GetMapping("/form-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 목록 조회")
    public List<DraftFormTemplateResponse> listDraftFormTemplates(@RequestParam(required = false) String businessType,
                                                                  @RequestParam(required = false) String organizationCode,
                                                                  @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return templateAdminService.listDraftFormTemplates(businessType, organizationCode, activeOnly, context, true).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    // Draft Template Presets
    @PostMapping("/template-presets")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "템플릿 프리셋 등록")
    public DraftTemplatePresetResponse createDraftTemplatePreset(@Valid @RequestBody DraftTemplatePresetRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftTemplatePresetResponse.apply(
                templateAdminService.createDraftTemplatePreset(request, context, true),
                masker);
    }

    @PutMapping("/template-presets/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "템플릿 프리셋 수정")
    public DraftTemplatePresetResponse updateDraftTemplatePreset(@PathVariable UUID id,
                                                                  @Valid @RequestBody DraftTemplatePresetRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftTemplatePresetResponse.apply(
                templateAdminService.updateDraftTemplatePreset(id, request, context, true),
                masker);
    }

    @GetMapping("/template-presets")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "템플릿 프리셋 목록 조회")
    public List<DraftTemplatePresetResponse> listDraftTemplatePresets(@RequestParam(required = false) String businessType,
                                                                      @RequestParam(required = false) String organizationCode,
                                                                      @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return templateAdminService.listDraftTemplatePresets(businessType, organizationCode, activeOnly, context, true).stream()
                .map(r -> DraftTemplatePresetResponse.apply(r, masker))
                .toList();
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
