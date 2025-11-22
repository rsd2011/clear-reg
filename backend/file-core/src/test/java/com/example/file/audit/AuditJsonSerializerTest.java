package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuditJsonSerializerTest {

    private static final ObjectMapper ORIGINAL;
    static {
        try {
            ORIGINAL = AuditJsonSerializer.objectMapper;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void restoreObjectMapper() throws Exception {
        AuditJsonSerializer.setObjectMapper(ORIGINAL);
    }

    @Test
    @DisplayName("정상 이벤트는 JSON 문자열로 직렬화된다")
    void serializeSuccess() {
        FileAuditEvent event = new FileAuditEvent("DOWNLOAD", UUID.randomUUID(), "alice", OffsetDateTime.now());

        String json = AuditJsonSerializer.serialize(event);

        assertThat(json).contains("DOWNLOAD");
    }

    @Test
    @DisplayName("JsonProcessingException 발생 시 action 값만 포함해 fallback JSON을 반환한다")
    void serializeFallbackOnException() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenThrow(new JsonProcessingException("boom") {});
        AuditJsonSerializer.setObjectMapper(failing);

        FileAuditEvent event = new FileAuditEvent("UPLOAD", UUID.randomUUID(), "bob", OffsetDateTime.now());

        String json = AuditJsonSerializer.serialize(event);

        assertThat(json).isEqualTo("{\"action\":\"UPLOAD\"}");
    }

    private void setObjectMapper(ObjectMapper mapper) {
        AuditJsonSerializer.setObjectMapper(mapper);
    }
}
