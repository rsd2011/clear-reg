package com.example.audit.drm;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
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
    @Getter(lombok.AccessLevel.NONE)
    Set<String> tags;
    String organizationCode;

    public Set<String> getTags() {
        return tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }

    public static class DrmAuditEventBuilder {
        public DrmAuditEventBuilder tags(Set<String> tags) {
            this.tags = tags == null ? Collections.emptySet() : Set.copyOf(tags);
            return this;
        }
    }
}
