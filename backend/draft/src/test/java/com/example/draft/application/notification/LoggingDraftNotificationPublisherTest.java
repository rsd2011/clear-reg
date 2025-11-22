package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingDraftNotificationPublisherTest {

    LoggingDraftNotificationPublisher publisher = new LoggingDraftNotificationPublisher();

    @Test
    @DisplayName("로그 퍼블리셔는 예외 없이 알림을 기록한다")
    void publish_doesNotThrow() {
        DraftNotificationPayload payload = new DraftNotificationPayload(
                UUID.randomUUID(),
                "CREATE",
                "actor",
                "creator",
                "ORG",
                "BF",
                UUID.randomUUID(),
                "delegatee",
                "memo",
                java.time.OffsetDateTime.now(),
                java.util.List.of("user1"));

        assertThatCode(() -> publisher.publish(payload)).doesNotThrowAnyException();
    }
}
