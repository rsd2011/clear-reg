package com.example.draft.infra.kafka;

import com.example.approval.api.event.DraftSubmittedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "approval.kafka", name = "enabled", havingValue = "true")
public class DraftSubmittedEventBridge {

    private final DraftSubmittedKafkaPublisher publisher;

    public DraftSubmittedEventBridge(DraftSubmittedKafkaPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void handle(DraftSubmittedEvent event) {
        publisher.publish(event);
    }
}
