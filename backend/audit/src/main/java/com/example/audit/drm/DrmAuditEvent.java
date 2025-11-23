package com.example.audit.drm;

import java.time.Instant;
import java.util.Set;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DrmAuditEvent {
    String assetId;
    DrmEventType eventType; // REQUEST, APPROVAL, EXECUTE
    String reasonCode;
    String reasonText;
    String requestorId;
    String approverId;
    Instant expiresAt;
    String route; // download/export/mail/print etc
    Set<String> tags;
    String organizationCode;
}
