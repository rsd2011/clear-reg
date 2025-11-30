package com.example.admin.draft.dto;

import com.example.common.orggroup.WorkType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 기안 양식 템플릿 생성/수정 요청 DTO.
 *
 * @param name          템플릿 이름
 * @param workType      업무 유형
 * @param schemaJson    폼 스키마 JSON (DraftFormSchema 직렬화)
 * @param active        활성화 여부
 * @param componentPath Vue 컴포넌트 경로 (Nuxt 4 프론트엔드용)
 * @param changeReason  변경 사유
 */
public record DraftFormTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull WorkType workType,
        @NotBlank String schemaJson,
        boolean active,
        @Size(max = 255) String componentPath,
        @Size(max = 500) String changeReason
) {
    /**
     * componentPath 없이 생성하는 간편 생성자.
     */
    public DraftFormTemplateRequest(String name, WorkType workType, String schemaJson,
                                     boolean active, String changeReason) {
        this(name, workType, schemaJson, active, null, changeReason);
    }
}
