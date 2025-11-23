package com.example.dw.application.job;

import com.example.dw.application.job.DwIngestionJobType;

/**
 * Outbox 메시지 직렬화를 위한 최소 페이로드.
 * 향후 필요시 feedId, tenant, correlationId 등을 확장할 수 있다.
 */
public record DwIngestionOutboxPayload(DwIngestionJobType jobType) {
}
