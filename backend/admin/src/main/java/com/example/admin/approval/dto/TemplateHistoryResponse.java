package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * 승인선 템플릿 변경 이력 응답 DTO.
 */
public record TemplateHistoryResponse(
        UUID id,
        String action,
        String changedBy,
        String changedByName,
        OffsetDateTime changedAt,
        Map<String, Object> changes
) {
    public static TemplateHistoryResponse apply(TemplateHistoryResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new TemplateHistoryResponse(
                response.id(),
                response.action(),
                fn.apply(response.changedBy()),
                fn.apply(response.changedByName()),
                response.changedAt(),
                response.changes()
        );
    }
}
