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

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.draft.application.TemplateAdminService;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.draft.application.request.ApprovalLineTemplateRequest;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.draft.application.response.ApprovalLineTemplateResponse;
import com.example.draft.application.response.DraftFormTemplateResponse;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/draft-admin")
public class DraftTemplateAdminController {

    private final TemplateAdminService templateAdminService;

    public DraftTemplateAdminController(TemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    // Approval Group
    @PostMapping("/approval-groups")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public ApprovalGroupResponse createGroup(@Valid @RequestBody ApprovalGroupRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.createApprovalGroup(request, context, true);
    }

    @PutMapping("/approval-groups/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public ApprovalGroupResponse updateGroup(@PathVariable UUID id,
                                             @Valid @RequestBody ApprovalGroupRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.updateApprovalGroup(id, request, context, true);
    }

    @GetMapping("/approval-groups")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<ApprovalGroupResponse> listGroups(@RequestParam(required = false) String organizationCode) {
        AuthContext context = currentContext();
        return templateAdminService.listApprovalGroups(organizationCode, context, true);
    }

    // Approval Line Templates
    @PostMapping("/approval-line-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public ApprovalLineTemplateResponse createApprovalLineTemplate(@Valid @RequestBody ApprovalLineTemplateRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.createApprovalLineTemplate(request, context, true);
    }

    @PutMapping("/approval-line-templates/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public ApprovalLineTemplateResponse updateApprovalLineTemplate(@PathVariable UUID id,
                                                                    @Valid @RequestBody ApprovalLineTemplateRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.updateApprovalLineTemplate(id, request, context, true);
    }

    @GetMapping("/approval-line-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<ApprovalLineTemplateResponse> listApprovalLineTemplates(@RequestParam(required = false) String businessType,
                                                                         @RequestParam(required = false) String organizationCode,
                                                                         @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        return templateAdminService.listApprovalLineTemplates(businessType, organizationCode, activeOnly, context, true);
    }

    // Draft Form Templates
    @PostMapping("/form-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public DraftFormTemplateResponse createDraftFormTemplate(@Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.createDraftFormTemplate(request, context, true);
    }

    @PutMapping("/form-templates/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public DraftFormTemplateResponse updateDraftFormTemplate(@PathVariable UUID id,
                                                             @Valid @RequestBody DraftFormTemplateRequest request) {
        AuthContext context = currentContext();
        return templateAdminService.updateDraftFormTemplate(id, request, context, true);
    }

    @GetMapping("/form-templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<DraftFormTemplateResponse> listDraftFormTemplates(@RequestParam(required = false) String businessType,
                                                                  @RequestParam(required = false) String organizationCode,
                                                                  @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        return templateAdminService.listDraftFormTemplates(businessType, organizationCode, activeOnly, context, true);
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
