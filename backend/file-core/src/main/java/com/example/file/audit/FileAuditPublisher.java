package com.example.file.audit;

public interface FileAuditPublisher {
    void publish(FileAuditEvent event);
}
