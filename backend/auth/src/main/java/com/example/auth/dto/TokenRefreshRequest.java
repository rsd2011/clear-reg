package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank(message = "refresh token is required") String refreshToken) {
}
