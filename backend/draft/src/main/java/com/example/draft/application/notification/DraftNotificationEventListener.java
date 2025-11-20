package com.example.draft.application.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DraftNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(DraftNotificationEventListener.class);

    private final List<DraftNotificationChannel> channels;

    public DraftNotificationEventListener(List<DraftNotificationChannel> channels) {
        this.channels = channels;
    }

    @EventListener
    public void onNotification(DraftNotificationPayload payload) {
        if (channels.isEmpty()) {
            log.warn("No notification channels configured; skipping draft notification for {}", payload.draftId());
            return;
        }
        channels.forEach(channel -> channel.send(payload));
    }
}
