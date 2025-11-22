package com.example.draft.application.audit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxDraftAuditRelayEmptyTest {

    @Test
    @DisplayName("Outbox가 비어 있으면 update를 호출하지 않는다")
    void noUpdateWhenEmpty() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        given(jdbc.queryForList(org.mockito.ArgumentMatchers.anyString())).willReturn(List.of());

        OutboxDraftAuditRelay relay = new OutboxDraftAuditRelay(jdbc);
        relay.relay();

        verify(jdbc, never()).update(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.<Object[]>any()
        );
    }
}
