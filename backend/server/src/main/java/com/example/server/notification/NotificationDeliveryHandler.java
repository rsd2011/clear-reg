package com.example.server.notification;

public interface NotificationDeliveryHandler {

    boolean supports(NotificationChannel channel);

    void handle(UserNotification notification);
}
