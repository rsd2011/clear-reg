package com.example.server.web;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.service.PermissionEvaluator;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.dto.DraftCreateRequest;
import com.example.draft.application.dto.DraftDecisionRequest;
import com.example.draft.application.dto.DraftResponse;
import com.example.draft.application.dto.DraftTemplateSuggestionResponse;
import com.example.draft.application.dto.DraftHistoryResponse;
import com.example.draft.application.dto.DraftReferenceResponse;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/drafts")
@Tag(name = "Draft", description = "기안 생성/조회/결재 API")
public class DraftController {

    private final DraftApplicationService draftApplicationService;
    private final PermissionEvaluator permissionEvaluator;
    private final DwOrganizationQueryService organizationQueryService;
    private static final RowScope DEFAULT_ROW_SCOPE = RowScope.ORG;

    public DraftController(DraftApplicationService draftApplicationService,
                           PermissionEvaluator permissionEvaluator,
                           DwOrganizationQueryService organizationQueryService) {
        this.draftApplicationService = draftApplicationService;
        this.permissionEvaluator = permissionEvaluator;
        this.organizationQueryService = organizationQueryService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public Page<DraftResponse> listDrafts(Pageable pageable,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false, name = "businessFeature") String businessFeatureCode,
                                          @RequestParam(required = false) String createdBy,
                                          @RequestParam(required = false) String title) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        RowScope rowScope = audit ? RowScope.ALL : effectiveRowScope(context.rowScope());
        Collection<String> scopedOrganizations = resolveOrganizations(rowScope, context.organizationCode());
        return draftApplicationService.listDrafts(pageable, rowScope, context.organizationCode(), scopedOrganizations,
                status, businessFeatureCode, createdBy, title);
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
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_SUBMIT);
        return draftApplicationService.submitDraft(id, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/approve")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse approveDraft(@PathVariable UUID id,
                                      @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.approve(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/reject")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse rejectDraft(@PathVariable UUID id,
                                     @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.reject(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/defer")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse deferDraft(@PathVariable UUID id,
                                    @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.defer(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/defer/approve")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_APPROVE)
    public DraftResponse approveDeferredDraft(@PathVariable UUID id,
                                              @Valid @RequestBody DraftDecisionRequest request) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_APPROVE);
        return draftApplicationService.approveDeferred(id, request, context.username(), context.organizationCode(), audit);
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CANCEL)
    public DraftResponse cancelDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_CANCEL);
        return draftApplicationService.cancel(id, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/withdraw")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_WITHDRAW)
    public DraftResponse withdrawDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_WITHDRAW);
        return draftApplicationService.withdraw(id, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/resubmit")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_RESUBMIT)
    public DraftResponse resubmitDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), hasAuditPermission());
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_RESUBMIT);
        return draftApplicationService.resubmit(id, context.username(), context.organizationCode());
    }

    @PostMapping("/{id}/delegate")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_DELEGATE)
    public DraftResponse delegateDraft(@PathVariable UUID id,
                                       @Valid @RequestBody DraftDecisionRequest request,
                                       @RequestParam("delegatedTo") String delegatedTo) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        DraftResponse snapshot = draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
        ensureBusinessPermission(snapshot.businessFeatureCode(), ActionCode.DRAFT_DELEGATE);
        return draftApplicationService.delegate(id, request, delegatedTo, context.username(), context.organizationCode(), audit);
    }

    @GetMapping("/templates/default")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public DraftTemplateSuggestionResponse defaultTemplates(@RequestParam("businessFeature") String businessFeature) {
        AuthContext context = currentContext();
        ensureBusinessPermission(businessFeature, ActionCode.DRAFT_CREATE);
        return draftApplicationService.suggestTemplate(businessFeature, context.organizationCode());
    }

    @GetMapping("/templates")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public List<DraftFormTemplateResponse> listTemplates(@RequestParam("businessFeature") String businessFeature) {
        AuthContext context = currentContext();
        ensureBusinessPermission(businessFeature, ActionCode.DRAFT_CREATE);
        var match = com.example.common.policy.MaskingContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return draftApplicationService.listFormTemplates(businessFeature, context.organizationCode()).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @GetMapping("/templates/recommend")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public List<DraftFormTemplateResponse> recommendTemplates(@RequestParam("businessFeature") String businessFeature) {
        AuthContext context = currentContext();
        ensureBusinessPermission(businessFeature, ActionCode.DRAFT_CREATE);
        var match = com.example.common.policy.MaskingContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return draftApplicationService.recommendFormTemplates(businessFeature, context.organizationCode(), context.username()).stream()
                .map(t -> DraftFormTemplateResponse.apply(t, masker))
                .toList();
    }

    @GetMapping("/{id}/history")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public List<DraftHistoryResponse> history(@PathVariable UUID id) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        return draftApplicationService.listHistory(id, context.organizationCode(), context.username(), audit);
    }

    @GetMapping("/{id}/references")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public List<DraftReferenceResponse> references(@PathVariable UUID id) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        return draftApplicationService.listReferences(id, context.organizationCode(), context.username(), audit);
    }

    @GetMapping("/{id}/audit")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<DraftHistoryResponse> audit(@PathVariable UUID id,
                                            @RequestParam(required = false) String action,
                                            @RequestParam(required = false) String actor,
                                            @RequestParam(required = false) OffsetDateTime from,
                                            @RequestParam(required = false) OffsetDateTime to) {
        AuthContext context = currentContext();
        return draftApplicationService.listAudit(id, context.organizationCode(), context.username(), true, action, actor, from, to);
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public DraftResponse getDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        boolean audit = hasAuditPermission();
        return draftApplicationService.getDraft(id, context.organizationCode(), context.username(), audit);
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

    private RowScope effectiveRowScope(RowScope requested) {
        if (requested == null) {
            return DEFAULT_ROW_SCOPE;
        }
        if (requested == RowScope.OWN) {
            return RowScope.ORG;
        }
        return requested;
    }
}
