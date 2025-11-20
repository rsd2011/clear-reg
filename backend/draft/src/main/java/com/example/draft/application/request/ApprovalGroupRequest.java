package com.example.draft.application.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalGroupRequest(
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(max = 64) String organizationCode,
        @Size(max = 1000) String conditionExpression
) {
}
