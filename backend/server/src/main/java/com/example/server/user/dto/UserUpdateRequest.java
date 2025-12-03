package com.example.server.user.dto;

import jakarta.validation.constraints.Email;
import java.util.Set;

/**
 * 사용자 수정 요청 DTO.
 */
public record UserUpdateRequest(
    @Email(message = "유효한 이메일 형식이어야 합니다")
    String email,

    String organizationCode,

    String permissionGroupCode,

    String employeeId,

    Set<String> roles
) {
}
