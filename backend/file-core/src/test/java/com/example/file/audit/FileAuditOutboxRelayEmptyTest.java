package com.example.file.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FileAuditOutboxRelayEmptyTest {

    @Test
    @DisplayName("대기 중 레코드가 없으면 퍼블리셔를 호출하지 않는다")
    void relay_skipsWhenNoRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        FileAuditPublisher publisher = mock(FileAuditPublisher.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyInt()))
                .thenReturn(List.of());

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbcTemplate, publisher, 10);

        relay.relay();

        verifyNoInteractions(publisher);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}
