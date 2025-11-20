package com.example.draft.application.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Outbox-friendly publisher placeholder (application event 기반).
 * 카프카/SQS 등 외부 브로커 사용 시 다른 publisher로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "draft.notification.publisher", havingValue = "event", matchIfMissing = true)
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
