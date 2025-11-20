package com.example.server.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

@RestController
@RequestMapping("/api/admin/draft")
public class DraftAdminController {

    private final ApprovalLineTemplateRepository approvalLineTemplateRepository;
    private final DraftFormTemplateRepository draftFormTemplateRepository;
    private final ApprovalGroupRepository approvalGroupRepository;

    public DraftAdminController(ApprovalLineTemplateRepository approvalLineTemplateRepository,
                                DraftFormTemplateRepository draftFormTemplateRepository,
                                ApprovalGroupRepository approvalGroupRepository) {
        this.approvalLineTemplateRepository = approvalLineTemplateRepository;
        this.draftFormTemplateRepository = draftFormTemplateRepository;
        this.approvalGroupRepository = approvalGroupRepository;
    }

    @GetMapping("/templates/approval")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<ApprovalLineTemplateSummary> listApprovalTemplates(@RequestParam(required = false) String businessType) {
        return approvalLineTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || t.getBusinessType().equalsIgnoreCase(businessType))
                .map(ApprovalLineTemplateSummary::from)
                .toList();
    }

    @GetMapping("/templates/form")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<DraftFormTemplateSummary> listFormTemplates(@RequestParam(required = false) String businessType) {
        return draftFormTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || t.getBusinessType().equalsIgnoreCase(businessType))
                .map(DraftFormTemplateSummary::from)
                .toList();
    }

    @GetMapping("/groups")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    public List<ApprovalGroupSummary> listGroups(@RequestParam(required = false) String organizationCode) {
        return approvalGroupRepository.findAll().stream()
                .filter(g -> organizationCode == null || g.getOrganizationCode().equalsIgnoreCase(organizationCode))
                .map(ApprovalGroupSummary::from)
                .toList();
    }

    public record ApprovalLineTemplateSummary(java.util.UUID id,
                                              String templateCode,
                                              String name,
                                              String businessType,
                                              String scope,
                                              String organizationCode,
                                              boolean active) {
        public static ApprovalLineTemplateSummary from(ApprovalLineTemplate template) {
            return new ApprovalLineTemplateSummary(template.getId(),
                    template.getTemplateCode(),
                    template.getName(),
                    template.getBusinessType(),
                    template.getScope().name(),
                    template.getOrganizationCode(),
                    template.isActive());
        }
    }

    public record DraftFormTemplateSummary(java.util.UUID id,
                                           String templateCode,
                                           String name,
                                           String businessType,
                                           String scope,
                                           String organizationCode,
                                           boolean active,
                                           int version) {
        public static DraftFormTemplateSummary from(DraftFormTemplate template) {
            return new DraftFormTemplateSummary(template.getId(),
                    template.getTemplateCode(),
                    template.getName(),
                    template.getBusinessType(),
                    template.getScope().name(),
                    template.getOrganizationCode(),
                    template.isActive(),
                    template.getVersion());
        }
    }

    public record ApprovalGroupSummary(java.util.UUID id,
                                       String groupCode,
                                       String name,
                                       String organizationCode,
                                       boolean active) {
        public static ApprovalGroupSummary from(ApprovalGroup group) {
            return new ApprovalGroupSummary(group.getId(),
                    group.getGroupCode(),
                    group.getName(),
                    group.getOrganizationCode(),
                    true);
        }
    }
}
