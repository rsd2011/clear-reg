package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditPartitionSchedulerTest {

    @Test
    @DisplayName("다음달 파티션 생성 DDL을 실행한다")
    void createsNextMonthPartition() throws Exception {
        DataSource ds = Mockito.mock(DataSource.class);
        Connection conn = Mockito.mock(Connection.class);
        Statement stmt = Mockito.mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        Clock clock = Clock.fixed(LocalDate.of(2025, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditPartitionScheduler scheduler = new AuditPartitionScheduler(ds, clock);

        scheduler.createNextMonthPartition();

        verify(stmt).execute(Mockito.contains("audit_log_2025_02"));
    }
}
