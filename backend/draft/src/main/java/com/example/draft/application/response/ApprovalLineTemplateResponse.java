package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.TemplateScope;

public record ApprovalLineTemplateResponse(
        UUID id,
        String templateCode,
        String name,
        String businessType,
        TemplateScope scope,
        String organizationCode,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ApprovalTemplateStepResponse> steps
) {
    public static ApprovalLineTemplateResponse from(ApprovalLineTemplate template) {
        return from(template, UnaryOperator.identity());
    }

    public static ApprovalLineTemplateResponse from(ApprovalLineTemplate template, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        List<ApprovalTemplateStepResponse> stepResponses = template.getSteps().stream()
                .map(ApprovalTemplateStepResponse::from)
                .toList();
        return new ApprovalLineTemplateResponse(
                template.getId(),
                fn.apply(template.getTemplateCode()),
                fn.apply(template.getName()),
                template.getBusinessType(),
                template.getScope(),
                template.getOrganizationCode(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                stepResponses
        );
    }

    public static ApprovalLineTemplateResponse apply(ApprovalLineTemplateResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalLineTemplateResponse(
                response.id(),
                fn.apply(response.templateCode()),
                fn.apply(response.name()),
                response.businessType(),
                response.scope(),
                response.organizationCode(),
                response.active(),
                response.createdAt(),
                response.updatedAt(),
                response.steps());
    }
}
