package com.example.draft.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftApprovalState;

public record DraftApprovalStepResponse(UUID id,
                                        int stepOrder,
                                        String approvalGroupCode,
                                        String description,
                                        DraftApprovalState state,
                                        String actedBy,
                                        OffsetDateTime actedAt,
                                        String comment,
                                        String delegatedTo,
                                        OffsetDateTime delegatedAt) {

    public static DraftApprovalStepResponse from(DraftApprovalStep step) {
        return new DraftApprovalStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getApprovalGroupCode(),
                step.getDescription(),
                step.getState(),
                step.getActedBy(),
                step.getActedAt(),
                step.getComment(),
                step.getDelegatedTo(),
                step.getDelegatedAt());
    }
}
