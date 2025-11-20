package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;

public record DraftFormTemplateResponse(
        UUID id,
        String templateCode,
        String name,
        String businessType,
        TemplateScope scope,
        String organizationCode,
        String schemaJson,
        int version,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DraftFormTemplateResponse from(DraftFormTemplate template) {
        return new DraftFormTemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getName(),
                template.getBusinessType(),
                template.getScope(),
                template.getOrganizationCode(),
                template.getSchemaJson(),
                template.getVersion(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
