package com.example.common.masking;

public interface UnmaskAuditSink {
    void handle(UnmaskAuditEvent event);
}
