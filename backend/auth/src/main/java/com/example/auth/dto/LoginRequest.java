package com.example.auth.dto;

import jakarta.validation.constraints.NotNull;

import com.example.auth.LoginType;

public record LoginRequest(
        @NotNull(message = "login type is required") LoginType type,
        String username,
        String password,
        String token
) {
}
