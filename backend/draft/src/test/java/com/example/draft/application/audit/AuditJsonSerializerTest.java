package com.example.draft.application.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditJsonSerializerTest {

    @Test
    @DisplayName("AuditJsonSerializer는 DraftAuditEvent를 JSON 문자열로 직렬화한다")
    void serializeEvent() {
        DraftAuditEvent event = new DraftAuditEvent(
                com.example.draft.domain.DraftAction.SUBMITTED,
                UUID.randomUUID(),
                "actor",
                "ORG",
                "comment",
                "127.0.0.1",
                "UA",
                null
        );

        String json = AuditJsonSerializer.serialize(event);

        assertThat(json)
                .contains("\"draftId\"")
                .contains("\"actor\"", "\"SUBMITTED\"")
                .contains(event.draftId().toString());
    }
}
