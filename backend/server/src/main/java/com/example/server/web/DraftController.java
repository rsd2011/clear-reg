package com.example.server.web;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.PermissionEvaluator;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.request.DraftCreateRequest;
import com.example.draft.application.request.DraftDecisionRequest;
import com.example.draft.application.response.DraftResponse;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/drafts")
public class DraftController {

    private final DraftApplicationService draftApplicationService;
    private final PermissionEvaluator permissionEvaluator;
    private final DwOrganizationQueryService organizationQueryService;

    public DraftController(DraftApplicationService draftApplicationService,
                           PermissionEvaluator permissionEvaluator,
                           DwOrganizationQueryService organizationQueryService) {
        this.draftApplicationService = draftApplicationService;
        this.permissionEvaluator = permissionEvaluator;
        this.organizationQueryService = organizationQueryService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public Page<DraftResponse> listDrafts(Pageable pageable) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        RowScope rowScope = audit ? RowScope.ALL : context.rowScope();
        Collection<String> scopedOrganizations = resolveOrganizations(rowScope, context.organizationCode());
        return draftApplicationService.listDrafts(pageable, rowScope, context.organizationCode(), scopedOrganizations);
    }

    @PostMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public DraftResponse createDraft(@Valid @RequestBody DraftCreateRequest request) {
        AuthContext context = currentContext();
        ensureBusinessPermission(request.businessFeatureCode(), ActionCode.DRAFT_CREATE);
        return draftApplicationService.createDraft(request, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/submit")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_SUBMIT)
    public DraftResponse submitDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_SUBMIT);
        return draftApplicationService.submitDraft(id, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/approve")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse approveDraft(@PathVariable UUID id,
                                      @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.approve(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/reject")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse rejectDraft(@PathVariable UUID id,
                                     @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.reject(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CANCEL)
    public DraftResponse cancelDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_CANCEL);
        return draftApplicationService.cancel(id, context.username(), context.organizationCode());
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public DraftResponse getDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        return draftApplicationService.getDraft(id, context.organizationCode(), audit);
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current()
                .orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }

    private Collection<String> resolveOrganizations(RowScope rowScope, String organizationCode) {
        if (rowScope == RowScope.ALL) {
            return List.of();
        }
        return organizationQueryService.getOrganizations(Pageable.unpaged(), rowScope, organizationCode)
                .map(DwOrganizationNode::organizationCode)
                .getContent();
    }

    private void ensureBusinessPermission(String featureCode, ActionCode actionCode) {
        try {
            FeatureCode feature = FeatureCode.valueOf(featureCode);
            permissionEvaluator.evaluate(feature, actionCode);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("알 수 없는 FeatureCode 입니다: " + featureCode);
        }
    }

    private boolean hasAuditPermission() {
        try {
            permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT);
            return true;
        } catch (PermissionDeniedException ex) {
            return false;
        }
    }
}
