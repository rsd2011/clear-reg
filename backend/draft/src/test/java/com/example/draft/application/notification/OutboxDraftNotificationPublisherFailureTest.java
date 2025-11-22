package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OutboxDraftNotificationPublisherFailureTest {

    @Test
    @DisplayName("이벤트 발행 실패 시 예외를 그대로 전파한다")
    void propagatesExceptionWhenPublishFails() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OutboxDraftNotificationPublisher publisher = new OutboxDraftNotificationPublisher(eventPublisher);
        DraftNotificationPayload payload = new DraftNotificationPayload(
                java.util.UUID.randomUUID(),
                "ACTION",
                "actor",
                "creator",
                "ORG",
                "FEATURE",
                null,
                null,
                null,
                java.time.OffsetDateTime.now(),
                java.util.List.of("creator")
        );

        doThrow(new RuntimeException("fail")).when(eventPublisher).publishEvent(payload);

        assertThatThrownBy(() -> publisher.publish(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fail");
    }
}
