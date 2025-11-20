package com.example.file.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "file.audit.publisher", havingValue = "log")
public class LoggingFileAuditPublisher implements FileAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingFileAuditPublisher.class);

    @Override
    public void publish(FileAuditEvent event) {
        log.info("file-audit action={} fileId={} actor={} at={}", event.action(), event.fileId(), event.actor(), event.occurredAt());
    }
}
