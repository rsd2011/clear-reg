package com.example.server.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationDeliveryListenerMoreTest {

    @Test
    @DisplayName("핸들러 리스트가 비어 있으면 아무 일도 하지 않는다")
    void emptyHandlers_noOp() {
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of());
        UserNotification notification = UserNotification.create(
                "user", "title", "msg", NotificationSeverity.INFO, NotificationChannel.IN_APP,
                OffsetDateTime.now(), "actor", null, null);
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, notification);

        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("지원하지 않는 채널이면 핸들러를 호출하지 않는다")
    void unsupportedChannel_skipsHandler() {
        NotificationDeliveryHandler email = Mockito.mock(NotificationDeliveryHandler.class);
        when(email.supports(NotificationChannel.EMAIL)).thenReturn(true);
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of(email));
        UserNotification notification = UserNotification.create(
                "user", "title", "msg", NotificationSeverity.INFO, NotificationChannel.IN_APP,
                OffsetDateTime.now(), "actor", null, null);
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, notification);

        listener.handle(event);

        verify(email, never()).handle(notification);
    }

    @Test
    @DisplayName("EMAIL 채널이면 이메일 핸들러를 호출한다")
    void emailChannel_invokesEmailHandler() {
        NotificationDeliveryHandler email = Mockito.mock(NotificationDeliveryHandler.class);
        when(email.supports(NotificationChannel.EMAIL)).thenReturn(true);
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of(email));
        UserNotification notification = UserNotification.create(
                "user", "title", "msg", NotificationSeverity.INFO, NotificationChannel.EMAIL,
                OffsetDateTime.now(), "actor", null, null);
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, notification);

        listener.handle(event);

        verify(email).handle(notification);
    }
}
