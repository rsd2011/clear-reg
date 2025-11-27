package com.example.draft.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.draft.domain.DraftReference;

public record DraftReferenceResponse(
        UUID id,
        String referencedUserId,
        String referencedOrgCode,
        String addedBy,
        OffsetDateTime addedAt
) {
    public static DraftReferenceResponse from(DraftReference ref) {
        return from(ref, UnaryOperator.identity());
    }

    public static DraftReferenceResponse from(DraftReference ref, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DraftReferenceResponse(
                ref.getId(),
                fn.apply(ref.getReferencedUserId()),
                ref.getReferencedOrgCode(),
                ref.getAddedBy(),
                ref.getAddedAt()
        );
    }
}
