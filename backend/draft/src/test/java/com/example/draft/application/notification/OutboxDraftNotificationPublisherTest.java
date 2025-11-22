package com.example.draft.application.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEventPublisher;

class OutboxDraftNotificationPublisherTest {

    ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);

    @Test
    @DisplayName("Outbox 퍼블리셔는 저장소에 위임한다")
    void publishDelegatesToRepository() {
        OutboxDraftNotificationPublisher outboxPublisher = new OutboxDraftNotificationPublisher(publisher);
        DraftNotificationPayload payload = new DraftNotificationPayload(null, "SUBMITTED", "actor", "creator", "ORG", "BF", null, null, null, java.time.OffsetDateTime.now(), java.util.List.of());

        outboxPublisher.publish(payload);

        Mockito.verify(publisher).publishEvent(payload);
    }
}
