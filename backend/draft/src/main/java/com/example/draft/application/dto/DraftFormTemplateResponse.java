package com.example.draft.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        return from(template, UnaryOperator.identity());
    }

    public static DraftFormTemplateResponse from(DraftFormTemplate template, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DraftFormTemplateResponse(
                template.getId(),
                fn.apply(template.getTemplateCode()),
                fn.apply(template.getName()),
                template.getBusinessType(),
                template.getScope(),
                template.getOrganizationCode(),
                fn.apply(template.getSchemaJson()),
                template.getVersion(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public static DraftFormTemplateResponse apply(DraftFormTemplateResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DraftFormTemplateResponse(
                response.id(),
                fn.apply(response.templateCode()),
                fn.apply(response.name()),
                response.businessType(),
                response.scope(),
                response.organizationCode(),
                fn.apply(response.schemaJson()),
                response.version(),
                response.active(),
                response.createdAt(),
                response.updatedAt());
    }
}
