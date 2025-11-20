package com.example.draft.application.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AuditJsonSerializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    private AuditJsonSerializer() {}

    static String serialize(DraftAuditEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit event", e);
        }
    }
}
