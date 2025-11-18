package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "current password is required") String currentPassword,
        @NotBlank(message = "new password is required") String newPassword
) {
}
