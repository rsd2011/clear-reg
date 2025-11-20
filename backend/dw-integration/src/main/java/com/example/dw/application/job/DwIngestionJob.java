package com.example.dw.application.job;

import java.util.UUID;

public record DwIngestionJob(UUID outboxId, DwIngestionJobType type) {

    public static DwIngestionJob fetchNext() {
        return new DwIngestionJob(null, DwIngestionJobType.FETCH_NEXT);
    }

    public static DwIngestionJob fromOutbox(UUID outboxId, DwIngestionJobType type) {
        return new DwIngestionJob(outboxId, type);
    }

    public boolean hasOutboxReference() {
        return outboxId != null;
    }

    public String payload() {
        return null; // reserved: payload is carried in outbox entity for now
    }
}
