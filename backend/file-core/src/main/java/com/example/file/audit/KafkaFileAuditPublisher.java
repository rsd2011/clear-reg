package com.example.file.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "file.audit.publisher", havingValue = "kafka")
public class KafkaFileAuditPublisher implements FileAuditPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaFileAuditPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   @org.springframework.beans.factory.annotation.Value("${file.audit.kafka.topic:file-audit-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(FileAuditEvent event) {
        kafkaTemplate.send(topic, event.fileId().toString(), AuditJsonSerializer.serialize(event));
    }
}
