package com.example.server.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest
@Import({NotificationService.class, NotificationServiceTest.TestConfig.class})
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Test
    @DisplayName("Given 수신자 목록 When send 호출 Then 알림이 저장된다")
    void givenRecipients_whenSend_thenPersistNotifications() {
        NotificationSendCommand command = new NotificationSendCommand(
                List.of("alice", "bob"),
                "테스트 알림",
                "내용",
                NotificationSeverity.INFO,
                NotificationChannel.IN_APP,
                null,
                null);

        notificationService.send(command, "system");

        assertThat(notificationRepository.count()).isEqualTo(2);
        var notifications = notificationService.notificationsFor("alice");
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    @DisplayName("Given 저장된 알림 When 읽음 처리 Then 상태와 시각이 갱신된다")
    void givenNotification_whenMarkRead_thenStatusUpdated() {
        NotificationSendCommand command = new NotificationSendCommand(
                List.of("reader"),
                "알림",
                "메시지",
                NotificationSeverity.INFO,
                NotificationChannel.IN_APP,
                null,
                null);
        notificationService.send(command, "system");
        UserNotification notification = notificationRepository.findAll().get(0);

        notificationService.markAsRead(notification.getId(), "reader");

        UserNotification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(updated.getReadAt()).isEqualTo(OffsetDateTime.ofInstant(TestConfig.NOW, ZoneOffset.UTC));
    }

    static class TestConfig {

        static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return event -> {
            };
        }
    }
}
