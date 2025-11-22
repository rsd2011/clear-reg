package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingDraftNotificationChannelTest {

    @Test
    @DisplayName("로그 채널은 예외 없이 payload를 기록한다")
    void sendsWithoutException() {
        LoggingDraftNotificationChannel channel = new LoggingDraftNotificationChannel();

        DraftNotificationPayload payload = new DraftNotificationPayload(
                UUID.randomUUID(),
                "ACTION",
                "actor",
                "creator",
                "ORG",
                "FEATURE",
                null,
                null,
                null,
                OffsetDateTime.now(),
                List.of("creator")
        );

        assertThatCode(() -> channel.send(payload)).doesNotThrowAnyException();
        // name()도 호출해 커버
        assertThatCode(channel::name).doesNotThrowAnyException();
    }
}
