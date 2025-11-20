package com.example.file.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AuditJsonSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AuditJsonSerializer() {
    }

    static String serialize(FileAuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"action\":\"" + event.action() + "\"}";
        }
    }
}
