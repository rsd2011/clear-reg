package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftReference;

public record DraftReferenceResponse(
        UUID id,
        String referencedUserId,
        String referencedOrgCode,
        String addedBy,
        OffsetDateTime addedAt
) {
    public static DraftReferenceResponse from(DraftReference ref) {
        return new DraftReferenceResponse(
                ref.getId(),
                ref.getReferencedUserId(),
                ref.getReferencedOrgCode(),
                ref.getAddedBy(),
                ref.getAddedAt()
        );
    }
}
