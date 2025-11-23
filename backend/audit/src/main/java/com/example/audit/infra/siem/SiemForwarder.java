package com.example.audit.infra.siem;

import com.example.audit.AuditEvent;

public interface SiemForwarder {
    void forward(AuditEvent event);
}
