package com.example.admin.draft.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.common.orggroup.WorkType;
import com.example.common.version.VersionStatus;

/**
 * 기안 양식 템플릿 목록 조회용 간략 응답 DTO.
 * schemaJson 등 대용량 필드를 제외한 요약 정보만 포함.
 */
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
    public static DraftFormTemplateSummary from(DraftFormTemplate template) {
        return from(template, UnaryOperator.identity());
    }

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
