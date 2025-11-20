package com.example.draft.application.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Outbox-friendly publisher placeholder.
 * For now, it emits an application event. A broker-backed publisher (Kafka/SQS) can replace this bean.
 */
@Component
public class OutboxDraftNotificationPublisher implements DraftNotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OutboxDraftNotificationPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(DraftNotificationPayload payload) {
        eventPublisher.publishEvent(payload);
    }
}
