package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FileAuditOutboxRowMapperTest {

    @Test
    @DisplayName("RowMapper는 ResultSet을 FileAuditOutboxRow로 변환한다")
    void mapRowBuildsRecord() throws Exception {
        ResultSet rs = Mockito.mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Timestamp ts = Timestamp.from(OffsetDateTime.now().toInstant());
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getString("action")).thenReturn("UPLOAD");
        when(rs.getObject("file_id", UUID.class)).thenReturn(fileId);
        when(rs.getString("actor")).thenReturn("actor");
        when(rs.getTimestamp("occurred_at")).thenReturn(ts);
        when(rs.getString("payload")).thenReturn("{}");

        var ctor = Class.forName("com.example.file.audit.FileAuditOutboxRelay$FileAuditOutboxRowMapper").getDeclaredConstructor();
        ctor.setAccessible(true);
        Object mapper = ctor.newInstance();
        var mapRow = mapper.getClass().getDeclaredMethod("mapRow", ResultSet.class, int.class);
        mapRow.setAccessible(true);
        Object row = mapRow.invoke(mapper, rs, 0);

        var idMethod = row.getClass().getDeclaredMethod("id");
        var actionMethod = row.getClass().getDeclaredMethod("action");
        var occurredMethod = row.getClass().getDeclaredMethod("occurredAt");

        assertThat(idMethod.invoke(row)).isEqualTo(id);
        assertThat(actionMethod.invoke(row)).isEqualTo("UPLOAD");
        assertThat(((OffsetDateTime) occurredMethod.invoke(row)).toInstant()).isEqualTo(ts.toInstant().atOffset(ZoneOffset.UTC).toInstant());
    }
}
