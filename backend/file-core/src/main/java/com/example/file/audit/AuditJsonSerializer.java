package com.example.file.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AuditJsonSerializer {
    // 변경: 테스트에서 실패 분기를 커버할 수 있도록 재할당 가능하도록 유지한다.
    static ObjectMapper objectMapper = new ObjectMapper();

    private AuditJsonSerializer() {
    }

    static String serialize(FileAuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"action\":\"" + event.action() + "\"}";
        }
    }

    // 테스트 전용 훅
    static void setObjectMapper(ObjectMapper mapper) {
        objectMapper = mapper;
    }
}
