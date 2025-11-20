package com.example.draft.application.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DraftCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotBlank String businessFeatureCode,
        @NotNull UUID templateId,
        @NotNull UUID formTemplateId,
        @NotBlank String formPayload,
        @Size(max = 10) List<@Valid DraftAttachmentRequest> attachments) {

    public DraftCreateRequest {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
