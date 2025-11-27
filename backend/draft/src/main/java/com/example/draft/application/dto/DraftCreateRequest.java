package com.example.draft.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DraftCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotBlank String businessFeatureCode,
        UUID templateId,
        UUID formTemplateId,
        @NotBlank String formPayload,
        @Size(max = 10) List<@Valid DraftAttachmentRequest> attachments,
        UUID templatePresetId,
        Map<String, String> templateVariables) {

    public DraftCreateRequest {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        templateVariables = templateVariables == null ? Map.of() : Map.copyOf(templateVariables);
    }
}
