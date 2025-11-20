package com.example.draft.application.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DraftAuditEvent(
        String action,
        UUID draftId,
        String actor,
        String organizationCode,
        String comment,
        String ip,
        String userAgent,
        OffsetDateTime occurredAt
) {
}
