package com.example.draft.infra.kafka;

import com.example.approval.api.event.DraftSubmittedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "approval.kafka", name = "enabled", havingValue = "true")
public class DraftSubmittedKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.example.approval.infra.config.ApprovalKafkaProperties props;

    public DraftSubmittedKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                        com.example.approval.infra.config.ApprovalKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publish(DraftSubmittedEvent event) {
        kafkaTemplate.send(props.getDraftSubmittedTopic(), event.draftId().toString(), event);
    }
}
