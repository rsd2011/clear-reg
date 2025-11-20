package com.example.draft.application.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "draft.notification.publisher", havingValue = "kafka")
public class KafkaDraftNotificationPublisher implements DraftNotificationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectFormatter formatter = new ObjectFormatter();

    public KafkaDraftNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = "draft-notifications";
    }

    @Override
    public void publish(DraftNotificationPayload payload) {
        kafkaTemplate.send(topic, payload.draftId().toString(), formatter.asJson(payload));
    }

    private static class ObjectFormatter {
        String asJson(DraftNotificationPayload payload) {
            return """
                    {
                      "draftId":"%s",
                      "action":"%s",
                      "actor":"%s",
                      "createdBy":"%s",
                      "organizationCode":"%s",
                      "businessFeatureCode":"%s",
                      "stepId":"%s",
                      "delegatedTo":"%s",
                      "comment":%s,
                      "occurredAt":"%s",
                      "recipients":%s
                    }
                    """.formatted(
                    payload.draftId(),
                    payload.action(),
                    payload.actor(),
                    payload.createdBy(),
                    payload.organizationCode(),
                    payload.businessFeatureCode(),
                    payload.stepId(),
                    payload.delegatedTo(),
                    payload.comment() == null ? null : "\"" + payload.comment().replace("\"", "\\\"") + "\"",
                    payload.occurredAt(),
                    toJsonArray(payload.recipients())
            );
        }

        private String toJsonArray(java.util.List<String> recipients) {
            if (recipients == null || recipients.isEmpty()) {
                return "[]";
            }
            return "[" + recipients.stream()
                    .map(r -> "\"" + r.replace("\"", "\\\"") + "\"")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("") + "]";
        }
    }
}
