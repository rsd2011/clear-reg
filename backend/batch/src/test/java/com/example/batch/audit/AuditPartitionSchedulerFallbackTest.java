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

class AuditPartitionSchedulerFallbackTest {

    @Test
    @DisplayName("PolicySettingsProvider가 null이어도 fallback 설정을 적용해 파티션을 생성한다")
    void createsPartitionWithFallbackWhenNoSettings() throws Exception {
        DataSource ds = Mockito.mock(DataSource.class);
        Connection conn = Mockito.mock(Connection.class);
        Statement stmt = Mockito.mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = () -> null; // currentSettings null

        Clock clock = Clock.fixed(LocalDate.of(2025, 3, 5).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditPartitionScheduler scheduler = new AuditPartitionScheduler(ds, clock, provider);
        setField(scheduler, "enabledFallback", true);
        setField(scheduler, "preloadMonthsFallback", 1);
        setField(scheduler, "hotTablespace", "audit_hot");

        scheduler.refreshOnPolicyChange(new AuditPartitionPolicyChangedEvent(AuditPartitionPolicyChangedEvent.AUDIT_POLICY_CODE));

        assertThatCode(scheduler::createNextPartitions).doesNotThrowAnyException();
        verify(stmt, Mockito.atLeastOnce()).execute(Mockito.contains("audit_log_2025_04"));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
