package com.example.admin.systemconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 시스템 설정 루트 생성/수정 요청 DTO.
 */
public record SystemConfigRootRequest(
    @NotBlank(message = "설정 코드는 필수입니다")
    @Pattern(regexp = "^[a-z][a-z0-9]*\\.[a-z][a-z0-9]*$", message = "설정 코드는 'xxx.xxx' 형식이어야 합니다")
    @Size(max = 100, message = "설정 코드는 100자 이내여야 합니다")
    String configCode,

    @NotBlank(message = "설정명은 필수입니다")
    @Size(max = 255, message = "설정명은 255자 이내여야 합니다")
    String name,

    @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
    String description,

    @NotBlank(message = "YAML 설정 내용은 필수입니다")
    String yamlContent,

    boolean active
) {
}
