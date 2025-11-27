package com.example.admin.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyYamlRequest(@NotBlank(message = "YAML payload is required") String yaml) {
}
