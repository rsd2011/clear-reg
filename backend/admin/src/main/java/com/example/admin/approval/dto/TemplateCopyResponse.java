package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.domain.ApprovalLineTemplate;

/**
 * 승인선 템플릿 복사 응답 DTO.
 * 복사된 템플릿 정보와 원본 참조 정보를 포함합니다.
 */
public record TemplateCopyResponse(
        UUID id,
        String templateCode,
        String name,
        Integer displayOrder,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ApprovalTemplateStepResponse> steps,
        CopiedFromInfo copiedFrom
) {
    /**
     * 원본 템플릿 참조 정보.
     */
    public record CopiedFromInfo(
            UUID id,
            String templateCode
    ) {
    }

    public static TemplateCopyResponse from(ApprovalLineTemplate copiedTemplate,
                                            ApprovalLineTemplate originalTemplate) {
        return from(copiedTemplate, originalTemplate, UnaryOperator.identity());
    }

    public static TemplateCopyResponse from(ApprovalLineTemplate copiedTemplate,
                                            ApprovalLineTemplate originalTemplate,
                                            UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        List<ApprovalTemplateStepResponse> stepResponses = copiedTemplate.getSteps().stream()
                .map(ApprovalTemplateStepResponse::from)
                .toList();

        return new TemplateCopyResponse(
                copiedTemplate.getId(),
                fn.apply(copiedTemplate.getTemplateCode()),
                fn.apply(copiedTemplate.getName()),
                copiedTemplate.getDisplayOrder(),
                fn.apply(copiedTemplate.getDescription()),
                copiedTemplate.isActive(),
                copiedTemplate.getCreatedAt(),
                copiedTemplate.getUpdatedAt(),
                stepResponses,
                new CopiedFromInfo(
                        originalTemplate.getId(),
                        fn.apply(originalTemplate.getTemplateCode())
                )
        );
    }

    public static TemplateCopyResponse apply(TemplateCopyResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new TemplateCopyResponse(
                response.id(),
                fn.apply(response.templateCode()),
                fn.apply(response.name()),
                response.displayOrder(),
                fn.apply(response.description()),
                response.active(),
                response.createdAt(),
                response.updatedAt(),
                response.steps(),
                new CopiedFromInfo(
                        response.copiedFrom().id(),
                        fn.apply(response.copiedFrom().templateCode())
                )
        );
    }
}
