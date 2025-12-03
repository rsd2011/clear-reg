package com.example.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO.
 *
 * <p>일반로그인 정책이 활성화된 경우에만 사용 가능합니다.
 */
public record PasswordChangeRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    String currentPassword,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    String newPassword
) {
}
