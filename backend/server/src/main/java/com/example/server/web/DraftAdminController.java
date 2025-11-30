package com.example.server.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.common.masking.MaskingFunctions;
import com.example.common.orggroup.WorkType;
import com.example.common.policy.MaskingContextHolder;
import com.example.common.version.VersionStatus;
import com.example.admin.draft.domain.DraftFormTemplate;
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
    public List<DraftFormTemplateSummary> listFormTemplates(@RequestParam(required = false) WorkType workType) {
        var maskMatch = MaskingContextHolder.get();
        var masker = MaskingFunctions.masker(maskMatch);
        List<DraftFormTemplate> templates;
        if (workType != null) {
            templates = draftFormTemplateRepository.findCurrentByWorkType(workType);
        } else {
            templates = draftFormTemplateRepository.findAllCurrent();
        }
        return templates.stream()
                .map(t -> DraftFormTemplateSummary.from(t, masker))
                .toList();
    }

    public record DraftFormTemplateSummary(
            UUID id,
            String templateCode,
            String name,
            WorkType workType,
            boolean active,
            Integer version,
            VersionStatus status,
            OffsetDateTime validFrom,
            OffsetDateTime validTo
    ) {
        public static DraftFormTemplateSummary from(DraftFormTemplate template, UnaryOperator<String> masker) {
            UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
            return new DraftFormTemplateSummary(
                    template.getId(),
                    fn.apply(template.getTemplateCode()),
                    fn.apply(template.getName()),
                    template.getWorkType(),
                    template.isActive(),
                    template.getVersion(),
                    template.getStatus(),
                    template.getValidFrom(),
                    template.getValidTo()
            );
        }
    }
}
