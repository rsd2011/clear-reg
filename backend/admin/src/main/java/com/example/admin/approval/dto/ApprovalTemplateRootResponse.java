package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;

public record ApprovalTemplateRootResponse(
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
    public static ApprovalTemplateRootResponse from(ApprovalTemplateRoot template) {
        return from(template, UnaryOperator.identity());
    }

    public static ApprovalTemplateRootResponse from(ApprovalTemplateRoot template, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        // currentVersion에서 steps를 가져옴
        ApprovalTemplate currentVersion = template.getCurrentVersion();
        List<ApprovalTemplateStepResponse> stepResponses = currentVersion != null
                ? currentVersion.getSteps().stream()
                        .map(ApprovalTemplateStepResponse::from)
                        .toList()
                : List.of();

        return new ApprovalTemplateRootResponse(
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

    public static ApprovalTemplateRootResponse apply(ApprovalTemplateRootResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalTemplateRootResponse(
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
