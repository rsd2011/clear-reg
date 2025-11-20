package com.example.draft.application.notification;

public interface DraftNotificationChannel {

    String name();

    void send(DraftNotificationPayload payload);
}
