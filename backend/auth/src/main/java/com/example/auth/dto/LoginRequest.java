package com.example.auth.dto;

import com.example.auth.LoginType;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotNull(message = "login type is required") LoginType type,
    String username,
    String password,
    String token) {}
