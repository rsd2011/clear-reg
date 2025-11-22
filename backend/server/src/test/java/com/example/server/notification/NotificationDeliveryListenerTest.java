package com.example.server.notification;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationDeliveryListenerTest {

    @Test
    @DisplayName("핸들러가 없으면 아무 일도 일어나지 않는다")
    void noHandlers_noops() {
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of());
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, sample(NotificationChannel.IN_APP));

        listener.handle(event);
    }

    @Test
    @DisplayName("지원하지 않는 채널이면 handle가 호출되지 않는다")
    void unsupportedChannel_skips() {
        NotificationDeliveryHandler handler = Mockito.mock(NotificationDeliveryHandler.class);
        given(handler.supports(NotificationChannel.EMAIL)).willReturn(false);
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of(handler));
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, sample(NotificationChannel.EMAIL));

        listener.handle(event);

        verify(handler, never()).handle(event.notification());
    }

    @Test
    @DisplayName("지원하는 채널이면 handle가 호출된다")
    void supportedChannel_handles() {
        NotificationDeliveryHandler handler = Mockito.mock(NotificationDeliveryHandler.class);
        given(handler.supports(NotificationChannel.EMAIL)).willReturn(true);
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of(handler));
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, sample(NotificationChannel.EMAIL));

        listener.handle(event);

        verify(handler).handle(event.notification());
    }

    @Test
    @DisplayName("첫 handler가 예외를 던지면 예외가 전파되고 이후 handler는 호출되지 않는다")
    void handlerThrows_stopsProcessing() {
        NotificationDeliveryHandler first = Mockito.mock(NotificationDeliveryHandler.class);
        NotificationDeliveryHandler second = Mockito.mock(NotificationDeliveryHandler.class);
        given(first.supports(NotificationChannel.EMAIL)).willReturn(true);
        given(second.supports(NotificationChannel.EMAIL)).willReturn(true);
        Mockito.doThrow(new RuntimeException("boom")).when(first).handle(any());
        NotificationDeliveryListener listener = new NotificationDeliveryListener(List.of(first, second));
        UserNotificationCreatedEvent event = new UserNotificationCreatedEvent(this, sample(NotificationChannel.EMAIL));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.handle(event))
                .isInstanceOf(RuntimeException.class);
        verify(second, never()).handle(any());
    }

    private UserNotification sample(NotificationChannel channel) {
        return UserNotification.create("user", "title", "msg", NotificationSeverity.INFO,
                channel, OffsetDateTime.now(), "system", null, null);
    }
}
