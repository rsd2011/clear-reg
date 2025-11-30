package com.example.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.draft.schema.DraftFormSchema;
import com.example.admin.draft.schema.builder.FormSchemaBuilders;
import com.example.admin.draft.dto.RollbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import com.example.admin.draft.service.TemplateAdminService;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.common.orggroup.WorkType;

@RestController
@Validated
@RequestMapping("/api/draft-admin")
@Tag(name = "Draft Template Admin", description = "기안 폼/승인선/프리셋 관리 API")
public class DraftTemplateAdminController {

    private final TemplateAdminService templateAdminService;
    private final ObjectMapper objectMapper;

    public DraftTemplateAdminController(TemplateAdminService templateAdminService,
                                         ObjectMapper objectMapper) {
        this.templateAdminService = templateAdminService;
        this.objectMapper = objectMapper;
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
    public List<DraftFormTemplateResponse> listDraftFormTemplates(@RequestParam(required = false) WorkType workType,
                                                                  @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return templateAdminService.listDraftFormTemplates(workType, activeOnly, context, true).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @GetMapping("/form-templates/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 단건 조회")
    public DraftFormTemplateResponse getDraftFormTemplate(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(templateAdminService.findById(id), masker);
    }

    @DeleteMapping("/form-templates/{rootId}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 삭제 (비활성화)")
    public ResponseEntity<Void> deleteDraftFormTemplate(@PathVariable UUID rootId) {
        AuthContext context = currentContext();
        templateAdminService.deleteTemplate(rootId, context);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/form-templates/root/{rootId}/versions")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 버전 히스토리 조회")
    public List<DraftFormTemplateResponse> getVersionHistory(@PathVariable UUID rootId) {
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return templateAdminService.getVersionHistory(rootId).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @PostMapping("/form-templates/{id}/rollback")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "기안 양식 템플릿 특정 버전으로 롤백")
    public DraftFormTemplateResponse rollbackToVersion(@PathVariable UUID id,
                                                        @Valid @RequestBody RollbackRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(com.example.common.policy.MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(
                templateAdminService.rollbackToVersion(id, request.changeReason(), context, request.overwriteDraft()),
                masker);
    }

    @GetMapping("/form-template-schemas/{workType}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "업무 유형별 기본 폼 스키마 조회")
    public String getDefaultSchema(@PathVariable WorkType workType) {
        DraftFormSchema schema = FormSchemaBuilders.forWorkType(workType);
        return schema.toJson(objectMapper);
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
