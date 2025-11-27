package com.example.admin.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 승인선 템플릿 복사 요청 DTO.
 */
public record TemplateCopyRequest(
        @NotBlank(message = "템플릿 이름은 필수입니다")
        @Size(max = 255, message = "템플릿 이름은 255자를 초과할 수 없습니다")
        String name,

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        String description
) {
}
