package com.example.draft.application.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalLineTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String businessType,
        @Size(max = 64) String organizationCode,
        boolean active,
        @NotNull @Size(min = 1) List<@Valid ApprovalTemplateStepRequest> steps
) {
}
