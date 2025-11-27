package com.example.admin.approval.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalLineTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @Min(0) Integer displayOrder,
        @Size(max = 500) String description,
        boolean active,
        @NotNull @Size(min = 1) List<@Valid ApprovalTemplateStepRequest> steps
) {
}
