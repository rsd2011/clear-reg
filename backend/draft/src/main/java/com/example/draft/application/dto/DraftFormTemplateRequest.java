package com.example.draft.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DraftFormTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String businessType,
        @Size(max = 64) String organizationCode,
        @NotBlank String schemaJson,
        boolean active
) {
}
