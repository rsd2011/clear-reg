package com.example.draft.application.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DraftNotificationPayload(
        UUID draftId,
        String action, // SUBMITTED, APPROVED, REJECTED, CANCELLED, WITHDRAWN, RESUBMITTED, DELEGATED
        String actor,
        String organizationCode,
        String businessFeatureCode,
        UUID stepId,
        String delegatedTo,
        String comment,
        OffsetDateTime occurredAt
) {
}
