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
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingFunctions;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContextHolder;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

@RestController
@RequestMapping("/api/admin/draft")
@Tag(name = "Draft Admin", description = "기안 템플릿 관리 API")
public class DraftAdminController {

    private final DraftFormTemplateRepository draftFormTemplateRepository;
    public DraftAdminController(DraftFormTemplateRepository draftFormTemplateRepository) {
        this.draftFormTemplateRepository = draftFormTemplateRepository;
    }

    @GetMapping("/templates/form")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<DraftFormTemplateSummary> listFormTemplates(@RequestParam(required = false) String businessType) {
        var match = DataPolicyContextHolder.get();
        var masker = MaskingFunctions.masker(match);
        RowScope scope = effectiveScope(match);
        var orgs = allowedOrganizations(scope);
        return draftFormTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || t.getBusinessType().equalsIgnoreCase(businessType))
                .filter(t -> filterOrg(scope, orgs, t.getOrganizationCode()))
                .map(t -> DraftFormTemplateSummary.from(t, masker))
                .toList();
    }


    private RowScope effectiveScope(com.example.common.policy.DataPolicyMatch match) {
        if (match != null && match.getRowScope() != null) {
            return RowScope.of(match.getRowScope(), RowScope.ALL);
        }
        return AuthContextHolder.current().map(c -> c.rowScope() != null ? c.rowScope() : RowScope.ALL).orElse(RowScope.ALL);
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

    public record DraftFormTemplateSummary(java.util.UUID id,
                                           String templateCode,
                                           String name,
                                           String businessType,
                                           String scope,
                                           String organizationCode,
                                           boolean active,
                                           int version) {
        public static DraftFormTemplateSummary from(DraftFormTemplate template, java.util.function.UnaryOperator<String> masker) {
            java.util.function.UnaryOperator<String> fn = masker == null ? java.util.function.UnaryOperator.identity() : masker;
            return new DraftFormTemplateSummary(template.getId(),
                    fn.apply(template.getTemplateCode()),
                    fn.apply(template.getName()),
                    template.getBusinessType(),
                    template.getScope().name(),
                    template.getOrganizationCode(),
                    template.isActive(),
                    template.getVersion());
        }
    }
}
