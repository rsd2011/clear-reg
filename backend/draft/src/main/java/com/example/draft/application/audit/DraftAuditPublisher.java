package com.example.draft.application.audit;

public interface DraftAuditPublisher {

    void publish(DraftAuditEvent event);
}
