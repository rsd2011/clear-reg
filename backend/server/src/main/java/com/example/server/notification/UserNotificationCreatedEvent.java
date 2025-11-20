package com.example.server.notification;

import org.springframework.context.ApplicationEvent;

public class UserNotificationCreatedEvent extends ApplicationEvent {

    private final UserNotification notification;

    public UserNotificationCreatedEvent(Object source, UserNotification notification) {
        super(source);
        this.notification = notification;
    }

    public UserNotification notification() {
        return notification;
    }
}
