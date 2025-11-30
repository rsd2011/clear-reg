package com.example.admin.draft.dto;

import java.util.UUID;

/**
 * 비즈니스 피처에 대한 기본 템플릿 추천 응답 DTO.
 *
 * @param approvalTemplateId   승인선 템플릿 ID
 * @param approvalTemplateCode 승인선 템플릿 코드
 * @param formTemplateId       기안 양식 템플릿 ID
 * @param formTemplateCode     기안 양식 템플릿 코드
 * @param organizational       조직별 매핑 여부 (true: 조직별, false: 전역)
 */
public record DraftTemplateSuggestionResponse(
        UUID approvalTemplateId,
        String approvalTemplateCode,
        UUID formTemplateId,
        String formTemplateCode,
        boolean organizational
) {
    /**
     * 값들을 직접 받아서 생성하는 정적 팩토리 메서드.
     */
    public static DraftTemplateSuggestionResponse of(
            UUID approvalTemplateId,
            String approvalTemplateCode,
            UUID formTemplateId,
            String formTemplateCode,
            String organizationCode) {
        return new DraftTemplateSuggestionResponse(
                approvalTemplateId,
                approvalTemplateCode,
                formTemplateId,
                formTemplateCode,
                organizationCode != null
        );
    }
}
