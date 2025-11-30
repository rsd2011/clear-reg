package com.example.draft.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.admin.draft.domain.DraftFormTemplate;

public record DraftFormTemplateResponse(
        UUID id,
        String templateCode,
        String name,
        WorkType workType,
        String schemaJson,
        Integer version,
        boolean active,
        VersionStatus status,
        ChangeAction changeAction,
        String changeReason,
        String changedBy,
        String changedByName,
        OffsetDateTime changedAt,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
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
                template.getWorkType(),
                fn.apply(template.getSchemaJson()),
                template.getVersion(),
                template.isActive(),
                template.getStatus(),
                template.getChangeAction(),
                template.getChangeReason(),
                template.getChangedBy(),
                template.getChangedByName(),
                template.getChangedAt(),
                template.getValidFrom(),
                template.getValidTo(),
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
                response.workType(),
                fn.apply(response.schemaJson()),
                response.version(),
                response.active(),
                response.status(),
                response.changeAction(),
                response.changeReason(),
                response.changedBy(),
                response.changedByName(),
                response.changedAt(),
                response.validFrom(),
                response.validTo(),
                response.createdAt(),
                response.updatedAt());
    }
}
