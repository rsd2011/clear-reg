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
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.dto.DraftFormTemplateSummary;
import com.example.admin.draft.dto.RollbackRequest;
import com.example.admin.draft.service.DraftFormTemplateService;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.common.masking.MaskingFunctions;
import com.example.common.orggroup.WorkType;
import com.example.common.policy.MaskingContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 기안 양식 템플릿 관리 API 컨트롤러.
 */
@RestController
@Validated
@RequestMapping("/api/admin/draft-form-templates")
@Tag(name = "Draft Form Template Admin", description = "기안 양식 템플릿 관리 API")
public class DraftFormTemplateController {

    private final DraftFormTemplateService templateService;
    private final ObjectMapper objectMapper;

    public DraftFormTemplateController(DraftFormTemplateService templateService,
                                        ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    // ==========================================================================
    // 기본 CRUD
    // ==========================================================================

    @PostMapping
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.CREATE)
    @Operation(summary = "기안 양식 템플릿 등록")
    public DraftFormTemplateResponse create(@Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(
                templateService.createDraftFormTemplate(request, context, true), masker);
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.UPDATE)
    @Operation(summary = "기안 양식 템플릿 수정")
    public DraftFormTemplateResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(
                templateService.updateDraftFormTemplate(id, request, context, true), masker);
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.READ)
    @Operation(summary = "기안 양식 템플릿 목록 조회")
    public List<DraftFormTemplateResponse> list(@RequestParam(required = false) WorkType workType,
                                                 @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return templateService.listDraftFormTemplates(workType, activeOnly, context, true).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @GetMapping("/summary")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.READ)
    @Operation(summary = "기안 양식 템플릿 목록 조회 (간략)")
    public List<DraftFormTemplateSummary> listSummary(@RequestParam(required = false) WorkType workType) {
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return templateService.listDraftFormTemplateSummaries(workType).stream()
                .map(t -> DraftFormTemplateSummary.from(t, masker))
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.READ)
    @Operation(summary = "기안 양식 템플릿 단건 조회")
    public DraftFormTemplateResponse get(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(templateService.findById(id), masker);
    }

    @DeleteMapping("/{rootId}")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.DELETE)
    @Operation(summary = "기안 양식 템플릿 삭제 (비활성화)")
    public ResponseEntity<Void> delete(@PathVariable UUID rootId) {
        AuthContext context = currentContext();
        templateService.deleteTemplate(rootId, context);
        return ResponseEntity.noContent().build();
    }

    // ==========================================================================
    // 버전 관리
    // ==========================================================================

    @GetMapping("/root/{rootId}/versions")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.READ)
    @Operation(summary = "기안 양식 템플릿 버전 히스토리 조회")
    public List<DraftFormTemplateResponse> getVersionHistory(@PathVariable UUID rootId) {
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return templateService.getVersionHistory(rootId).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @PostMapping("/{id}/rollback")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.UPDATE)
    @Operation(summary = "기안 양식 템플릿 특정 버전으로 롤백")
    public DraftFormTemplateResponse rollbackToVersion(@PathVariable UUID id,
                                                        @Valid @RequestBody RollbackRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(MaskingContextHolder.get());
        return DraftFormTemplateResponse.apply(
                templateService.rollbackToVersion(id, request.changeReason(), context, request.overwriteDraft()),
                masker);
    }

    // ==========================================================================
    // 스키마
    // ==========================================================================

    @GetMapping("/schemas/{workType}")
    @RequirePermission(feature = FeatureCode.DRAFT_TEMPLATE, action = ActionCode.READ)
    @Operation(summary = "업무 유형별 기본 폼 스키마 조회")
    public String getDefaultSchema(@PathVariable WorkType workType) {
        DraftFormSchema schema = FormSchemaBuilders.forWorkType(workType);
        return schema.toJson(objectMapper);
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current()
                .orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
