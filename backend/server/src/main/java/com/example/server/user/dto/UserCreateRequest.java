package com.example.server.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * 사용자 생성 요청 DTO.
 */
public record UserCreateRequest(
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 4, max = 50, message = "사용자명은 4~50자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 밑줄만 허용됩니다")
    String username,

    @Email(message = "유효한 이메일 형식이어야 합니다")
    String email,

    @NotBlank(message = "조직 코드는 필수입니다")
    String organizationCode,

    @NotBlank(message = "권한 그룹 코드는 필수입니다")
    String permissionGroupCode,

    String employeeId,

    Set<String> roles,

    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    String password
) {
}
