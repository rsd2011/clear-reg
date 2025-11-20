package com.example.draft.application.audit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDraftAuditPublisher implements DraftAuditPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringDraftAuditPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DraftAuditEvent event) {
        publisher.publishEvent(event);
    }
}
