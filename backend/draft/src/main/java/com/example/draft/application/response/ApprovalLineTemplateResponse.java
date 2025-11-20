package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
        List<ApprovalTemplateStepResponse> stepResponses = template.getSteps().stream()
                .map(ApprovalTemplateStepResponse::from)
                .toList();
        return new ApprovalLineTemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getName(),
                template.getBusinessType(),
                template.getScope(),
                template.getOrganizationCode(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                stepResponses
        );
    }
}
