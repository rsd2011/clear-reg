package com.example.draft.application.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "draft.notification.publisher", havingValue = "kafka")
public class KafkaDraftNotificationPublisher implements DraftNotificationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaDraftNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = "draft-notifications";
    }

    @Override
    public void publish(DraftNotificationPayload payload) {
        try {
            kafkaTemplate.send(topic, payload.draftId().toString(), objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize draft notification", e);
        }
    }
}
