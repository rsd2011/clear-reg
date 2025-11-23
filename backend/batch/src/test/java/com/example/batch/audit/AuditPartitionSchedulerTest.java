package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThatCode;
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

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditPartitionSchedulerTest {

    @Test
    @DisplayName("정책이 enable일 때 다음달 파티션을 생성한다")
    void createsPartitionWhenEnabled() throws Exception {
        DataSource ds = Mockito.mock(DataSource.class);
        Connection conn = Mockito.mock(Connection.class);
        Statement stmt = Mockito.mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = Mockito.mock(PolicySettingsProvider.class);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                true, true, true, 0, true, "MEDIUM", true, null, null,
                true, "0 0 2 1 * *", 0,
                true, "0 0 4 1 * *");
        when(provider.currentSettings()).thenReturn(toggles);

        Clock clock = Clock.fixed(LocalDate.of(2025, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditPartitionScheduler scheduler = new AuditPartitionScheduler(ds, clock, provider);

        assertThatCode(scheduler::createNextPartitions).doesNotThrowAnyException();
        verify(stmt).execute(Mockito.contains("audit_log_2025_02"));
    }

    @Test
    @DisplayName("정책이 비활성화면 실행하지 않는다")
    void disabledSkips() {
        PolicySettingsProvider provider = () -> new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                true, true, true, 0, true, "MEDIUM", true, null, null,
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *");
        AuditPartitionScheduler scheduler = new AuditPartitionScheduler(null, Clock.systemUTC(), provider);
        assertThatCode(scheduler::createNextPartitions).doesNotThrowAnyException();
    }
}
