package com.example.server.notification;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.notification.dto.NotificationSendCommand;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public NotificationService(UserNotificationRepository notificationRepository,
                               ApplicationEventPublisher eventPublisher,
                               Clock clock,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void send(NotificationSendCommand command, String actor) {
        List<String> recipients = command.recipients() == null ? Collections.emptyList() : command.recipients();
        OffsetDateTime now = now();
        for (String recipient : recipients) {
            String metadata = command.metadata() != null && !command.metadata().isEmpty()
                    ? compressMetadata(command.metadata())
                    : null;
            UserNotification notification = UserNotification.create(
                    recipient,
                    command.title(),
                    command.message(),
                    command.severity(),
                    command.channel(),
                    now,
                    actor,
                    command.link(),
                    metadata);
            UserNotification saved = notificationRepository.save(notification);
            eventPublisher.publishEvent(new UserNotificationCreatedEvent(this, saved));
        }
    }

    @Transactional(readOnly = true)
    public List<UserNotification> notificationsFor(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public void markAsRead(UUID id, String username) {
        UserNotification notification = notificationRepository.findByIdAndRecipientUsername(id, username)
                .orElseThrow(() -> new UserNotificationNotFoundException(id));
        if (notification.isUnread()) {
            notification.markRead(now());
            notificationRepository.save(notification);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private String compressMetadata(java.util.Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        }
        catch (JsonProcessingException e) {
            return metadata.toString();
        }
    }
}
