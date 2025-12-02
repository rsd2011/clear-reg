package com.example.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 시스템 설정 메타정보(이름, 설명) 수정 요청 DTO.
 */
public record SystemConfigInfoUpdateRequest(
    @NotBlank(message = "설정명은 필수입니다")
    @Size(max = 255, message = "설정명은 255자 이내여야 합니다")
    String name,

    @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
    String description
) {
}
