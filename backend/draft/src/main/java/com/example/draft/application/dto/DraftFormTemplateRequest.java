package com.example.draft.application.dto;

import com.example.common.orggroup.WorkType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DraftFormTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull WorkType workType,
        @NotBlank String schemaJson,
        boolean active,
        @Size(max = 500) String changeReason
) {
}
