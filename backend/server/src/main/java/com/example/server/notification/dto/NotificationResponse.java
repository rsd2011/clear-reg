package com.example.server.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        return from(notification, UnaryOperator.identity());
    }

    public static NotificationResponse from(UserNotification notification, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NotificationResponse(
                notification.getId(),
                fn.apply(notification.getTitle()),
                fn.apply(notification.getMessage()),
                notification.getSeverity(),
                notification.getChannel(),
                notification.getStatus(),
                fn.apply(notification.getLink()),
                fn.apply(notification.getMetadata()),
                notification.getCreatedAt(),
                notification.getReadAt());
    }

    public static NotificationResponse apply(NotificationResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NotificationResponse(
                response.id(),
                fn.apply(response.title()),
                fn.apply(response.message()),
                response.severity(),
                response.channel(),
                response.status(),
                fn.apply(response.link()),
                fn.apply(response.metadata()),
                response.createdAt(),
                response.readAt());
    }
}
