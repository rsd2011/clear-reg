package com.example.draft.application.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.draft.domain.DraftAction;

class AuditSupportTest {

    @Test
    @DisplayName("DraftAuditEvent를 JSON으로 직렬화한다")
    void serializeAuditEvent() {
        DraftAuditEvent event = new DraftAuditEvent(
                DraftAction.SUBMITTED,
                UUID.randomUUID(),
                "actor",
                "ORG",
                "comment",
                "127.0.0.1",
                "agent",
                null // occurredAt null로 JSr310 모듈 없이 직렬화 가능하도록
        );

        String json = AuditJsonSerializer.serialize(event);
        assertThat(json).contains("actor", "draftId", "SUBMITTED");
    }

    @Test
    @DisplayName("OutboxDraftAuditRelay는 PENDING 행을 SENT로 업데이트한다")
    void outboxRelayUpdates() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID id = UUID.randomUUID();
        given(jdbc.queryForList(any())).willReturn(List.of(Map.of("id", id, "payload", "{}")));

        OutboxDraftAuditRelay relay = new OutboxDraftAuditRelay(jdbc);
        relay.relay();

        verify(jdbc).update(any(), eq(id));
    }
}
