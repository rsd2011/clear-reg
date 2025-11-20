package com.example.server.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.server.notification.NotificationChannel;
import com.example.server.notification.NotificationSeverity;
import com.example.server.notification.NotificationStatus;
import com.example.server.notification.UserNotification;

public record NotificationResponse(UUID id,
                                   String title,
                                   String message,
                                   NotificationSeverity severity,
                                   NotificationChannel channel,
                                   NotificationStatus status,
                                   String link,
                                   String metadata,
                                   OffsetDateTime createdAt,
                                   OffsetDateTime readAt) {

    public static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSeverity(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getLink(),
                notification.getMetadata(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
