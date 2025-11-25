package com.example.server.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.common.masking.MaskingFunctions;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContextHolder;
import com.example.approval.domain.ApprovalGroup;
import com.example.approval.domain.ApprovalLineTemplate;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;

@RestController
@Tag(name = "Approval Admin", description = "승인 템플릿/그룹 관리 API")
@RequestMapping("/api/admin/approval")
public class ApprovalAdminController {

    private final ApprovalLineTemplateRepository approvalLineTemplateRepository;
    private final ApprovalGroupRepository approvalGroupRepository;

    public ApprovalAdminController(ApprovalLineTemplateRepository approvalLineTemplateRepository,
                                   ApprovalGroupRepository approvalGroupRepository) {
        this.approvalLineTemplateRepository = approvalLineTemplateRepository;
        this.approvalGroupRepository = approvalGroupRepository;
    }

    @GetMapping("/templates")
    @RequirePermission(feature = FeatureCode.APPROVAL, action = ActionCode.APPROVAL_ADMIN)
    public List<ApprovalLineTemplateSummary> listApprovalTemplates(@RequestParam(required = false) String businessType) {
        var match = DataPolicyContextHolder.get();
        var masker = MaskingFunctions.masker(match);
        RowScope scope = effectiveScope(match);
        var orgs = allowedOrganizations(scope);
        return approvalLineTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || t.getBusinessType().equalsIgnoreCase(businessType))
                .filter(t -> filterOrg(scope, orgs, t.getOrganizationCode()))
                .map(t -> ApprovalLineTemplateSummary.from(t, masker))
                .toList();
    }

    @GetMapping("/groups")
    @RequirePermission(feature = FeatureCode.APPROVAL, action = ActionCode.APPROVAL_ADMIN)
    public List<ApprovalGroupSummary> listGroups(@RequestParam(required = false) String organizationCode) {
        var match = DataPolicyContextHolder.get();
        var masker = MaskingFunctions.masker(match);
        RowScope scope = effectiveScope(match);
        var orgs = allowedOrganizations(scope);
        return approvalGroupRepository.findAll().stream()
                .filter(g -> organizationCode == null || g.getOrganizationCode().equalsIgnoreCase(organizationCode))
                .filter(g -> filterOrg(scope, orgs, g.getOrganizationCode()))
                .map(g -> ApprovalGroupSummary.from(g, masker))
                .toList();
    }

    private RowScope effectiveScope(com.example.common.policy.DataPolicyMatch match) {
        if (match != null && match.getRowScope() != null) {
            return RowScope.of(match.getRowScope(), RowScope.ALL);
        }
        return RowScope.ALL;
    }

    private java.util.Collection<String> allowedOrganizations(RowScope scope) {
        var ctx = RowScopeContextHolder.get();
        return ctx != null && ctx.organizationHierarchy() != null && !ctx.organizationHierarchy().isEmpty()
                ? ctx.organizationHierarchy()
                : ctx != null && ctx.organizationCode() != null ? java.util.List.of(ctx.organizationCode()) : java.util.List.of();
    }

    private boolean filterOrg(RowScope scope, java.util.Collection<String> orgs, String targetOrg) {
        return switch (scope) {
            case OWN -> targetOrg != null && orgs.contains(targetOrg);
            case ORG -> targetOrg != null && (orgs.isEmpty() || orgs.contains(targetOrg));
            default -> true;
        };
    }

    public record ApprovalLineTemplateSummary(java.util.UUID id,
                                              String templateCode,
                                              String name,
                                              String businessType,
                                              String scope,
                                              String organizationCode,
                                              boolean active) {
        public static ApprovalLineTemplateSummary from(ApprovalLineTemplate template, java.util.function.UnaryOperator<String> masker) {
            java.util.function.UnaryOperator<String> fn = masker == null ? java.util.function.UnaryOperator.identity() : masker;
            return new ApprovalLineTemplateSummary(template.getId(),
                    fn.apply(template.getTemplateCode()),
                    fn.apply(template.getName()),
                    template.getBusinessType(),
                    template.getScope().name(),
                    template.getOrganizationCode(),
                    template.isActive());
        }
    }

    public record ApprovalGroupSummary(java.util.UUID id,
                                       String groupCode,
                                       String name,
                                       String organizationCode,
                                       boolean active) {
        public static ApprovalGroupSummary from(ApprovalGroup group, java.util.function.UnaryOperator<String> masker) {
            java.util.function.UnaryOperator<String> fn = masker == null ? java.util.function.UnaryOperator.identity() : masker;
            return new ApprovalGroupSummary(group.getId(),
                    fn.apply(group.getGroupCode()),
                    fn.apply(group.getName()),
                    group.getOrganizationCode(),
                    true);
        }
    }
}
