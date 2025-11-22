package com.example.common.masking;

import java.time.Instant;
import java.util.Set;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnmaskAuditEvent {
    Instant eventTime;
    SubjectType subjectType;
    String dataKind;
    String fieldName;
    String rowId;
    Set<String> requesterRoles;
    String reason; // optional
}
