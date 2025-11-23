package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.draft.domain.DraftHistory;

public record DraftHistoryResponse(
        UUID id,
        String eventType,
        String actor,
        String details,
        OffsetDateTime occurredAt
) {
    public static DraftHistoryResponse from(DraftHistory history) {
        return from(history, UnaryOperator.identity());
    }

    public static DraftHistoryResponse from(DraftHistory history, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DraftHistoryResponse(
                history.getId(),
                history.getEventType(),
                history.getActor(),
                fn.apply(history.getDetails()),
                history.getOccurredAt()
        );
    }
}
