package com.example.admin.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalGroupRequest(
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String description,
        @Min(0) Integer priority
) {
}
