package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountStatusChangeRequest(
    @NotBlank(message = "username is required") String username, boolean active) {}
