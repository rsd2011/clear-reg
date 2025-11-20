package com.example.draft.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingDraftNotificationPublisher implements DraftNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDraftNotificationPublisher.class);

    @Override
    public void publish(DraftNotificationPayload payload) {
        log.info("Draft notification: action={} draftId={} actor={} org={} stepId={} delegatedTo={}",
                payload.action(), payload.draftId(), payload.actor(), payload.organizationCode(),
                payload.stepId(), payload.delegatedTo());
    }
}
