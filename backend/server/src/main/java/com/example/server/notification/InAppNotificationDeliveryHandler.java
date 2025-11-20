package com.example.server.notification;

import org.springframework.stereotype.Component;

@Component
public class InAppNotificationDeliveryHandler implements NotificationDeliveryHandler {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public void handle(UserNotification notification) {
        // No external delivery needed for in-app notifications (persisted + fetched via API)
    }
}
