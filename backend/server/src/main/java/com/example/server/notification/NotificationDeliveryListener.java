package com.example.server.notification;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryListener {

    private final List<NotificationDeliveryHandler> handlers;

    public NotificationDeliveryListener(List<NotificationDeliveryHandler> handlers) {
        this.handlers = handlers;
    }

    @EventListener
    public void handle(UserNotificationCreatedEvent event) {
        UserNotification notification = event.notification();
        handlers.stream()
                .filter(handler -> handler.supports(notification.getChannel()))
                .forEach(handler -> handler.handle(notification));
    }
}
