package com.example.server.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.common.masking.MaskingFunctions;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;

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
    public List<ApprovalLineTemplateSummary> listApprovalTemplates() {
        var match = DataPolicyContextHolder.get();
        var masker = MaskingFunctions.masker(match);
        return approvalLineTemplateRepository.findAll().stream()
                .map(t -> ApprovalLineTemplateSummary.from(t, masker))
                .toList();
    }

    @GetMapping("/groups")
    @RequirePermission(feature = FeatureCode.APPROVAL, action = ActionCode.APPROVAL_ADMIN)
    public List<ApprovalGroupSummary> listGroups(@RequestParam(defaultValue = "true") boolean activeOnly) {
        var match = DataPolicyContextHolder.get();
        var masker = MaskingFunctions.masker(match);
        return approvalGroupRepository.findAll().stream()
                .filter(g -> !activeOnly || g.isActive())
                .map(g -> ApprovalGroupSummary.from(g, masker))
                .toList();
    }


    public record ApprovalLineTemplateSummary(java.util.UUID id,
                                              String templateCode,
                                              String name,
                                              boolean active) {
        public static ApprovalLineTemplateSummary from(ApprovalLineTemplate template, java.util.function.UnaryOperator<String> masker) {
            java.util.function.UnaryOperator<String> fn = masker == null ? java.util.function.UnaryOperator.identity() : masker;
            return new ApprovalLineTemplateSummary(template.getId(),
                    fn.apply(template.getTemplateCode()),
                    fn.apply(template.getName()),
                    template.isActive());
        }
    }

    public record ApprovalGroupSummary(java.util.UUID id,
                                       String groupCode,
                                       String name,
                                       Integer priority,
                                       boolean active) {
        public static ApprovalGroupSummary from(ApprovalGroup group, java.util.function.UnaryOperator<String> masker) {
            java.util.function.UnaryOperator<String> fn = masker == null ? java.util.function.UnaryOperator.identity() : masker;
            return new ApprovalGroupSummary(group.getId(),
                    fn.apply(group.getGroupCode()),
                    fn.apply(group.getName()),
                    group.getDisplayOrder(),
                    group.isActive());
        }
    }
}
