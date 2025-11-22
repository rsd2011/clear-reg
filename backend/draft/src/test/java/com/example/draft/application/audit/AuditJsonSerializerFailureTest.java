package com.example.draft.application.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.DraftAction;

class AuditJsonSerializerFailureTest {

    @Test
    @DisplayName("occurredAt가 비어있지 않으면 OffsetDateTime 직렬화 실패를 IllegalStateException으로 감싼다")
    void throwsWhenOffsetDateTimeNotSupported() {
        DraftAuditEvent event = new DraftAuditEvent(
                DraftAction.SUBMITTED,
                UUID.randomUUID(),
                "actor",
                "ORG",
                "comment",
                "127.0.0.1",
                "UA",
                OffsetDateTime.now());

        assertThatThrownBy(() -> AuditJsonSerializer.serialize(event))
                .isInstanceOf(IllegalStateException.class);
    }
}
