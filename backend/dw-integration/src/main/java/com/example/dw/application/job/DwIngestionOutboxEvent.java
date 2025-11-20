package com.example.dw.application.job;

import java.util.UUID;

/**
 * Outbox → Broker 메시지 페이로드 표준형.
 */
public record DwIngestionOutboxEvent(UUID outboxId, DwIngestionJobType jobType, String payload) {
}
