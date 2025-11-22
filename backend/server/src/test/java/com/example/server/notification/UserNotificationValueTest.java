package com.example.server.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserNotificationValueTest {

    @Test
    @DisplayName("severity와 channel이 null이면 기본값 INFO/IN_APP을 사용한다")
    void defaultsAppliedWhenNull() {
        UserNotification notification = UserNotification.create(
                "user",
                "title",
                "msg",
                null,
                null,
                OffsetDateTime.now(),
                "actor",
                null,
                null);

        assertThat(notification.getSeverity()).isEqualTo(NotificationSeverity.INFO);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.IN_APP);
    }
}
