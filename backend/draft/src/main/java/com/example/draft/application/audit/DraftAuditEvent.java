package com.example.draft.application.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftAction;

public record DraftAuditEvent(
        DraftAction action,
        UUID draftId,
        String actor,
        String organizationCode,
        String comment,
        String ip,
        String userAgent,
        OffsetDateTime occurredAt
) {
}
