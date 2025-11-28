package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.domain.ApprovalLineTemplate;

public record ApprovalLineTemplateResponse(
        UUID id,
        String templateCode,
        String name,
        Integer displayOrder,
        String description,
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
                template.getDisplayOrder(),
                fn.apply(template.getDescription()),
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
                response.displayOrder(),
                fn.apply(response.description()),
                response.active(),
                response.createdAt(),
                response.updatedAt(),
                response.steps());
    }
}
