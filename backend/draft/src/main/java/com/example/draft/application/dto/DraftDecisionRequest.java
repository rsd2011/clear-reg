package com.example.draft.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DraftDecisionRequest(
        @NotNull UUID stepId,
        @Size(max = 2000) String comment) {
}
