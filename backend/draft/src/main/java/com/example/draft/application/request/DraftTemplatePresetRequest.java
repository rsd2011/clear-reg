package com.example.draft.application.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DraftTemplatePresetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank String businessFeatureCode,
        String organizationCode,
        @NotBlank String titleTemplate,
        @NotBlank String contentTemplate,
        @NotNull UUID formTemplateId,
        UUID defaultApprovalTemplateId,
        @NotBlank String defaultFormPayload,
        List<@NotBlank String> variables,
        boolean active
) {
    public DraftTemplatePresetRequest {
        variables = variables == null ? List.of() : List.copyOf(variables);
    }
}
