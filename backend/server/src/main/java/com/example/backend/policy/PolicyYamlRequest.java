package com.example.backend.policy;

import jakarta.validation.constraints.NotBlank;

public record PolicyYamlRequest(@NotBlank(message = "YAML payload is required") String yaml) {
}
