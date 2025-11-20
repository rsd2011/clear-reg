package com.example.server.notification;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoggingEmailNotificationHandler implements NotificationDeliveryHandler {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void handle(UserNotification notification) {
        log.info("[EMAIL] recipient={} title={} message={}",
                notification.getRecipientUsername(),
                notification.getTitle(),
                notification.getMessage());
    }
}
