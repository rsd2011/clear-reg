package com.example.draft.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DraftAttachmentRequest(
        @NotNull UUID fileId,
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 150) String contentType,
        @Positive long fileSize) {
}
