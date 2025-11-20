package com.example.file.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FileAuditPublisher.class)
public class NoOpFileAuditPublisher implements FileAuditPublisher {
    @Override
    public void publish(FileAuditEvent event) {
        // no-op
    }
}
