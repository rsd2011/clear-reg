package com.example.draft.application.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxDraftAuditPublisherTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    OutboxDraftAuditPublisher publisher;

    @Test
    @DisplayName("Outbox 퍼블리셔는 audit_outbox 테이블에 PENDING 행을 적재한다")
    void savesPendingOutboxRow() {
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

        publisher.publish(event);

        verify(jdbcTemplate).update(any(String.class), any(UUID.class), eq(event.draftId()), eq("DRAFT"), any(String.class), eq(event.occurredAt()));
    }
}
