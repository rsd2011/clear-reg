package com.example.draft.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "draft.notification.channel", havingValue = "log", matchIfMissing = true)
public class LoggingDraftNotificationChannel implements DraftNotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingDraftNotificationChannel.class);

    @Override
    public String name() {
        return "LOG";
    }

    @Override
    public void send(DraftNotificationPayload payload) {
        log.info("Notify recipients={} action={} draftId={} actor={} feature={} org={}",
                payload.recipients(), payload.action(), payload.draftId(), payload.actor(),
                payload.businessFeatureCode(), payload.organizationCode());
    }
}
