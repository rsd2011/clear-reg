package com.example.draft.application.dto;

import java.util.UUID;

import com.example.draft.domain.BusinessTemplateMapping;

public record DraftTemplateSuggestionResponse(
        UUID approvalTemplateId,
        String approvalTemplateCode,
        UUID formTemplateId,
        String formTemplateCode,
        boolean organizational
) {

    public static DraftTemplateSuggestionResponse from(BusinessTemplateMapping mapping) {
        return new DraftTemplateSuggestionResponse(
                mapping.getApprovalLineTemplate().getId(),
                mapping.getApprovalLineTemplate().getTemplateCode(),
                mapping.getDraftFormTemplate().getId(),
                mapping.getDraftFormTemplate().getTemplateCode(),
                mapping.getOrganizationCode() != null
        );
    }
}
