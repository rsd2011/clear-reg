package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.approval.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftTemplatePreset;
import com.example.draft.domain.TemplateScope;

public record DraftTemplatePresetResponse(
        UUID id,
        String presetCode,
        String name,
        String businessFeatureCode,
        TemplateScope scope,
        String organizationCode,
        String titleTemplate,
        String contentTemplate,
        UUID formTemplateId,
        String formTemplateCode,
        UUID defaultApprovalTemplateId,
        String defaultApprovalTemplateCode,
        String defaultFormPayload,
        List<String> variables,
        int version,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static DraftTemplatePresetResponse from(DraftTemplatePreset preset,
                                                   List<String> variables,
                                                   UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        ApprovalLineTemplate approvalTemplate = preset.getDefaultApprovalTemplate();
        return new DraftTemplatePresetResponse(
                preset.getId(),
                fn.apply(preset.getPresetCode()),
                fn.apply(preset.getName()),
                preset.getBusinessFeatureCode(),
                preset.getScope(),
                preset.getOrganizationCode(),
                fn.apply(preset.getTitleTemplate()),
                fn.apply(preset.getContentTemplate()),
                preset.getFormTemplate().getId(),
                fn.apply(preset.getFormTemplate().getTemplateCode()),
                approvalTemplate == null ? null : approvalTemplate.getId(),
                approvalTemplate == null ? null : fn.apply(approvalTemplate.getTemplateCode()),
                fn.apply(preset.getDefaultFormPayload()),
                variables == null ? List.of() : List.copyOf(variables),
                preset.getVersion(),
                preset.isActive(),
                preset.getCreatedAt(),
                preset.getUpdatedAt()
        );
    }

    public static DraftTemplatePresetResponse apply(DraftTemplatePresetResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DraftTemplatePresetResponse(
                response.id(),
                fn.apply(response.presetCode()),
                fn.apply(response.name()),
                response.businessFeatureCode(),
                response.scope(),
                response.organizationCode(),
                fn.apply(response.titleTemplate()),
                fn.apply(response.contentTemplate()),
                response.formTemplateId(),
                fn.apply(response.formTemplateCode()),
                response.defaultApprovalTemplateId(),
                response.defaultApprovalTemplateCode() == null ? null : fn.apply(response.defaultApprovalTemplateCode()),
                fn.apply(response.defaultFormPayload()),
                response.variables(),
                response.version(),
                response.active(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
