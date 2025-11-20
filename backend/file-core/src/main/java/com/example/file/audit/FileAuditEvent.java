package com.example.file.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FileAuditEvent(
        String action,
        UUID fileId,
        String actor,
        OffsetDateTime occurredAt
) {
}
