package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftHistory;

public record DraftHistoryResponse(
        UUID id,
        String eventType,
        String actor,
        String details,
        OffsetDateTime occurredAt
) {
    public static DraftHistoryResponse from(DraftHistory history) {
        return new DraftHistoryResponse(
                history.getId(),
                history.getEventType(),
                history.getActor(),
                history.getDetails(),
                history.getOccurredAt()
        );
    }
}
