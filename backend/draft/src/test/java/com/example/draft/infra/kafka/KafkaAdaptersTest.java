package com.example.draft.infra.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.approval.api.event.DraftSubmittedEvent;
import com.example.draft.application.approval.ApprovalCompletedEventListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

class KafkaAdaptersTest {

    @Test
    void draftSubmittedBridgePublishesToKafka() {
        DraftSubmittedKafkaPublisher publisher = Mockito.mock(DraftSubmittedKafkaPublisher.class);
        DraftSubmittedEventBridge bridge = new DraftSubmittedEventBridge(publisher);

        bridge.handle(new DraftSubmittedEvent(UUID.randomUUID(), "T", "ORG", "u", "s", List.of("G")));

        verify(publisher).publish(any());
    }

    @Test
    void draftSubmittedKafkaPublisherSendsToKafka() {
        var kafka = Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
        var props = new com.example.approval.infra.config.ApprovalKafkaProperties();
        DraftSubmittedKafkaPublisher publisher = new DraftSubmittedKafkaPublisher(kafka, props);

        DraftSubmittedEvent event = new DraftSubmittedEvent(UUID.randomUUID(), "T", "ORG", "u", "s", List.of("G"));
        publisher.publish(event);

        verify(kafka).send(props.getDraftSubmittedTopic(), event.draftId().toString(), event);
    }

    @Test
    void approvalCompletedKafkaListenerDelegates() {
        ApprovalCompletedEventListener delegate = Mockito.mock(ApprovalCompletedEventListener.class);
        ApprovalCompletedKafkaListener listener = new ApprovalCompletedKafkaListener(delegate, new com.example.approval.infra.config.ApprovalKafkaProperties());

        listener.onMessage(new com.example.approval.api.event.ApprovalCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), com.example.approval.api.ApprovalStatus.APPROVED, "actor", "c"));

        verify(delegate).handle(any());
    }
}
