package com.example.batch.ingestion.queue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.dw.application.job.DwIngestionOutboxEvent;
import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka 기반 Outbox 퍼블리셔.
 * dw.ingestion.outbox.publisher.type=kafka 일 때 활성화됩니다.
 */
@Component
@ConditionalOnProperty(prefix = "dw.ingestion.outbox.publisher", name = "type", havingValue = "kafka")
public class KafkaOutboxMessagePublisher implements OutboxMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public KafkaOutboxMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                       ObjectMapper objectMapper,
                                       @org.springframework.beans.factory.annotation.Value("${dw.ingestion.outbox.publisher.kafka.topic:dw-ingestion-jobs}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DwIngestionOutbox entry) {
        String key = entry.getId().toString();
        DwIngestionOutboxEvent event = new DwIngestionOutboxEvent(entry.getId(), entry.getJobType(), entry.getPayload());
        kafkaTemplate.send(topic, key, serialize(event));
    }

    private String serialize(DwIngestionOutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event " + event.outboxId(), e);
        }
    }
}
