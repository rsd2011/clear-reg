package com.example.server.notification;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;
import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "user_notifications")
@Getter
public class UserNotification extends PrimaryKeyEntity {

    @Column(name = "recipient_username", nullable = false, length = 100)
    private String recipientUsername;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NotificationSeverity severity = NotificationSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Version
    private long version;

    protected UserNotification() {
        // for JPA
    }

    public static UserNotification create(String recipientUsername,
                                          String title,
                                          String message,
                                          NotificationSeverity severity,
                                          NotificationChannel channel,
                                          OffsetDateTime createdAt,
                                          String createdBy,
                                          String link,
                                          String metadata) {
        UserNotification notification = new UserNotification();
        notification.recipientUsername = recipientUsername;
        notification.title = title;
        notification.message = message;
        notification.severity = severity == null ? NotificationSeverity.INFO : severity;
        notification.channel = channel == null ? NotificationChannel.IN_APP : channel;
        notification.status = NotificationStatus.UNREAD;
        notification.createdAt = createdAt;
        notification.createdBy = createdBy;
        notification.link = link;
        notification.metadata = metadata;
        return notification;
    }

    public void markRead(OffsetDateTime readAt) {
        this.status = NotificationStatus.READ;
        this.readAt = readAt;
    }

    public boolean isUnread() {
        return this.status == NotificationStatus.UNREAD;
    }

    public void clearReadAt() {
        this.readAt = null;
    }
}
