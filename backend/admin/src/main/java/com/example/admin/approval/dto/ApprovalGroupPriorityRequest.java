package com.example.admin.approval.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record ApprovalGroupPriorityRequest(
        @NotEmpty @Valid List<PriorityItem> priorities
) {
    public record PriorityItem(
            @NotNull UUID id,
            @NotNull @Min(0) Integer priority
    ) {
    }
}
