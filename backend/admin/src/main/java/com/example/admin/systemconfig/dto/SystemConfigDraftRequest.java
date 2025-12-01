package com.example.admin.systemconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 시스템 설정 초안 생성/수정 요청 DTO.
 */
public record SystemConfigDraftRequest(
    @NotBlank(message = "YAML 설정 내용은 필수입니다")
    String yamlContent,

    boolean active,

    @Size(max = 500, message = "변경 사유는 500자 이내여야 합니다")
    String changeReason
) {
}
