package com.example.admin.codegroup.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 마이그레이션 실행 요청 DTO.
 *
 * <p>DB의 CodeGroup을 새로운 groupCode로 일괄 변경합니다.</p>
 *
 * @param id 마이그레이션할 CodeGroup의 ID
 * @param newGroupCode 새 그룹 코드 (Enum에 존재)
 */
public record MigrationRequest(
        @NotNull(message = "그룹 ID는 필수입니다")
        UUID id,

        @NotBlank(message = "새 그룹 코드는 필수입니다")
        String newGroupCode
) {
}
