package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class FileAuditOutboxRelayTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    FileAuditPublisher publisher;

    @Mock
    ResultSet resultSet;

    @Test
    @DisplayName("outbox 레코드를 성공적으로 중계하면 SENT로 업데이트한다")
    void relaySuccessUpdatesStatus() throws Exception {
        FileAuditEvent event = new FileAuditEvent("UPLOAD", UUID.randomUUID(), "actor", OffsetDateTime.now());
        Object row = newOutboxRow(event, "{}");
        when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenReturn(List.of(row));

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbcTemplate, publisher, 10);

        relay.relay();

        verify(publisher).publish(any(FileAuditEvent.class));
        verify(jdbcTemplate).update("update file_audit_outbox set status = 'SENT', last_error = null where id = ?", getRowId(row));
    }

    @Test
    @DisplayName("중계 중 예외가 발생하면 FAILED로 표시한다")
    void relayFailureMarksFailed() throws Exception {
        FileAuditEvent event = new FileAuditEvent("UPLOAD", UUID.randomUUID(), "actor", OffsetDateTime.now());
        Object row = newOutboxRow(event, "{}");
        when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenReturn(List.of(row));
        doThrow(new RuntimeException("fail")).when(publisher).publish(any(FileAuditEvent.class));

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbcTemplate, publisher, 10);

        assertThatNoException().isThrownBy(relay::relay);
        verify(jdbcTemplate).update("update file_audit_outbox set status = 'FAILED', last_error = ? where id = ?", "fail", getRowId(row));
    }

    @Test
    @DisplayName("테이블이 없으면 BadSqlGrammarException을 전파한다")
    void relayWhenTableMissing_propagates() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenThrow(new org.springframework.jdbc.BadSqlGrammarException("", "", new java.sql.SQLException("table missing")));

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbcTemplate, publisher, 1);

        org.assertj.core.api.Assertions.assertThatThrownBy(relay::relay)
                .isInstanceOf(org.springframework.jdbc.BadSqlGrammarException.class);
    }

    private Object newOutboxRow(FileAuditEvent event, String payload) throws Exception {
        Class<?> rowClass = Class.forName("com.example.file.audit.FileAuditOutboxRelay$FileAuditOutboxRow");
        var ctor = rowClass.getDeclaredConstructor(UUID.class, String.class, UUID.class, String.class, OffsetDateTime.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(UUID.randomUUID(), event.action(), event.fileId(), event.actor(), event.occurredAt(), payload);
    }

    private Object getRowId(Object row) throws Exception {
        var method = row.getClass().getDeclaredMethod("id");
        method.setAccessible(true);
        return method.invoke(row);
    }
}
