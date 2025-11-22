package com.example.server.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDeliveryHandlerSupportTest {

    @Test
    @DisplayName("이메일 핸들러는 EMAIL만 지원하고 IN_APP은 거부한다")
    void loggingEmailHandler_supportsOnlyEmail() {
        LoggingEmailNotificationHandler handler = new LoggingEmailNotificationHandler();

        assertThat(handler.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(handler.supports(NotificationChannel.IN_APP)).isFalse();

        UserNotification notification = UserNotification.create(
                "user", "title", "msg", NotificationSeverity.INFO, NotificationChannel.EMAIL,
                OffsetDateTime.now(), "actor", null, null);

        assertThatCode(() -> handler.handle(notification)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("인앱 핸들러는 IN_APP만 지원하고 EMAIL은 거부한다")
    void inAppHandler_supportsOnlyInApp() {
        InAppNotificationDeliveryHandler handler = new InAppNotificationDeliveryHandler();

        assertThat(handler.supports(NotificationChannel.IN_APP)).isTrue();
        assertThat(handler.supports(NotificationChannel.EMAIL)).isFalse();

        UserNotification notification = UserNotification.create(
                "user", "title", "msg", NotificationSeverity.INFO, NotificationChannel.IN_APP,
                OffsetDateTime.now(), "actor", null, null);

        assertThatCode(() -> handler.handle(notification)).doesNotThrowAnyException();
    }
}
