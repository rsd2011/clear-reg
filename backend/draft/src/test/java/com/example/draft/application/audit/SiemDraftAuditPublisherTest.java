package com.example.draft.application.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiemDraftAuditPublisherTest {

    @Test
    @DisplayName("SIEM 퍼블리셔는 이벤트를 로깅하면서 예외를 던지지 않는다")
    void publishesToLogger() {
        SiemDraftAuditPublisher publisher = new SiemDraftAuditPublisher();
        DraftAuditEvent event = new DraftAuditEvent(
                com.example.draft.domain.DraftAction.SUBMITTED,
                UUID.randomUUID(),
                "actor",
                "ORG",
                "comment",
                "127.0.0.1",
                "ua",
                OffsetDateTime.now());

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
