package com.example.server.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

class NotificationServiceTest {

    private final UserNotificationRepository repository = Mockito.mock(UserNotificationRepository.class);
    private final ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final NotificationService service = new NotificationService(repository, eventPublisher, clock, new ObjectMapper());

    @Test
    @DisplayName("알림 전송 시 수신자마다 저장하고 이벤트를 발행한다")
    void sendPublishesEventPerRecipient() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationSendCommand command = new NotificationSendCommand(
                List.of("u1", "u2"),
                "title",
                "msg",
                NotificationSeverity.INFO,
                NotificationChannel.IN_APP,
                null,
                Map.of("k", "v"));

        service.send(command, "actor");

        verify(repository, Mockito.times(2)).save(any(UserNotification.class));
        verify(eventPublisher, Mockito.times(2)).publishEvent(any(UserNotificationCreatedEvent.class));
    }

    @Test
    @DisplayName("읽지 않은 알림만 읽음으로 변경한다")
    void markAsReadUpdatesUnread() {
        UUID id = UUID.randomUUID();
        UserNotification notification = UserNotification.create(
                "user", "t", "m", NotificationSeverity.INFO, NotificationChannel.IN_APP,
                OffsetDateTime.now(clock), "actor", null, null);
        when(repository.findByIdAndRecipientUsername(id, "user")).thenReturn(Optional.of(notification));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markAsRead(id, "user");

        assertThat(notification.isUnread()).isFalse();
        verify(repository).save(notification);
    }

    @Test
    @DisplayName("이미 읽은 알림은 다시 저장하지 않는다")
    void markAsReadSkipsWhenAlreadyRead() {
        UUID id = UUID.randomUUID();
        UserNotification notification = UserNotification.create(
                "user", "t", "m", NotificationSeverity.INFO, NotificationChannel.IN_APP,
                OffsetDateTime.now(clock), "actor", null, null);
        notification.markRead(OffsetDateTime.now(clock));
        when(repository.findByIdAndRecipientUsername(id, "user")).thenReturn(Optional.of(notification));

        service.markAsRead(id, "user");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("메타데이터가 null이면 압축을 건너뛰고 저장한다")
    void sendSkipsMetadataWhenNull() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationSendCommand command = new NotificationSendCommand(
                List.of("u1"),
                "title",
                "msg",
                NotificationSeverity.INFO,
                NotificationChannel.IN_APP,
                null,
                null);

        service.send(command, "actor");

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(repository).save(captor.capture());
        var field = UserNotification.class.getDeclaredField("metadata");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isNull();
    }

    @Test
    @DisplayName("알림이 없으면 notificationsFor는 빈 리스트를 반환한다")
    void notificationsForEmptyReturnsEmptyList() {
        when(repository.findByRecipientUsernameOrderByCreatedAtDesc("nouser")).thenReturn(List.of());

        assertThat(service.notificationsFor("nouser")).isEmpty();
    }
}
