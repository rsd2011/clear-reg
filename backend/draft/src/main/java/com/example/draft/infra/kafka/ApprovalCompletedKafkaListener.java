package com.example.draft.infra.kafka;

import com.example.approval.api.event.ApprovalCompletedEvent;
import com.example.draft.application.approval.ApprovalCompletedEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "approval.kafka", name = "enabled", havingValue = "true")
public class ApprovalCompletedKafkaListener {

    private final ApprovalCompletedEventListener delegate;
    private final com.example.approval.infra.config.ApprovalKafkaProperties props;

    public ApprovalCompletedKafkaListener(ApprovalCompletedEventListener delegate,
                                          com.example.approval.infra.config.ApprovalKafkaProperties props) {
        this.delegate = delegate;
        this.props = props;
    }

    @KafkaListener(topics = "${approval.kafka.approval-completed-topic:approval-completed}", groupId = "draft-service")
    public void onMessage(ApprovalCompletedEvent event) {
        delegate.handle(event);
    }
}
