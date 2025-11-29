package com.example.batch.audit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.mockito.Mockito;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditColdMaintenanceJobTest {

    private PolicyToggleSettings toggles(boolean coldArchiveEnabled) {
        return new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                true, true, true, 0, true, "MEDIUM", true, null, null,
                true, "0 0 2 1 * *", 0,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                coldArchiveEnabled, "0 30 2 2 * *",
                true, "0 30 3 * * *");
    }

    @Test
    @DisplayName("cold archive가 활성화되면 이전 달 파티션을 COLD TS로 이동하고 재색인한다")
    void movesPartitionWhenEnabled() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.currentSettings()).thenReturn(toggles(true));

        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, clock, provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "hotMonths", 0); // 바로 직전 달 대상

        job.moveOldPartitions();

        var ordered = inOrder(stmt);
        ordered.verify(stmt).execute(contains("ALTER TABLE audit_log_2025_04 SET TABLESPACE ts_cold"));
        ordered.verify(stmt).execute(contains("REINDEX TABLE audit_log_2025_04"));
    }

    @Test
    @DisplayName("policy disable이면 아무 작업도 하지 않는다")
    void skipsWhenDisabled() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.currentSettings()).thenReturn(toggles(false));

        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");

        job.moveOldPartitions();

        verify(stmt, never()).execute(anyString());
    }

    @Test
    @DisplayName("tablespace가 비어있으면 실행하지 않는다")
    void skipsWhenTablespaceBlank() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.currentSettings()).thenReturn(toggles(true));

        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "   ");

        job.moveOldPartitions();

        verify(stmt, never()).execute(anyString());
    }

    @Test
    @DisplayName("재색인 단계에서 예외가 발생해도 실패하지 않는다")
    void toleratesReindexFailure() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.execute(org.mockito.ArgumentMatchers.startsWith("REINDEX"))).thenThrow(new java.sql.SQLException("fail"));

        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.currentSettings()).thenReturn(toggles(true));

        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, clock, provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "hotMonths", 0);

        job.moveOldPartitions(); // should swallow reindex error

        verify(stmt).execute(contains("ALTER TABLE audit_log_2025_04 SET TABLESPACE ts_cold"));
    }

    @Test
    @DisplayName("테이블 이동 실패도 swallow한다")
    void toleratesAlterFailure() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.execute(org.mockito.ArgumentMatchers.startsWith("ALTER TABLE"))).thenThrow(new java.sql.SQLException("alter fail"));

        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.currentSettings()).thenReturn(toggles(true));

        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, clock, provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "hotMonths", 0);

        job.moveOldPartitions(); // should not throw despite alter failure
    }

    @Test
    @DisplayName("PolicyChangedEvent를 수신하면 로그만 남기고 예외 없이 동작한다")
    void handlesPolicyChangeEvent() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);

        job.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("security.policy", "yaml"));
        job.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("other", "yaml"));
    }

    @Test
    @DisplayName("batchJobSchedule이 있으면 trigger는 정책 스케줄을 사용한다")
    void triggerUsesPolicySchedule() {
        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_COLD_MAINTENANCE))
                .thenReturn(new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.CRON, "0 15 * * * *", 0, 0, null));

        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        org.assertj.core.api.Assertions.assertThat(job.trigger().expression()).isEqualTo("0 15 * * * *");
    }

    @Test
    @DisplayName("batchJobSchedule이 없으면 trigger는 currentCron을 사용한다")
    void triggerUsesFallbackWhenPolicyMissing() {
        PolicySettingsProvider provider = mock(PolicySettingsProvider.class);
        when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_COLD_MAINTENANCE))
                .thenReturn(null);
        when(provider.currentSettings()).thenReturn(toggles(true));

        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "cron", "0 0 5 * * 0");

        org.assertj.core.api.Assertions.assertThat(job.trigger().enabled()).isTrue();
        // toggles(true)에서 auditColdArchiveCron = "0 30 2 2 * *" 이므로 해당 값 반환
        org.assertj.core.api.Assertions.assertThat(job.trigger().expression()).isEqualTo("0 30 2 2 * *");
    }

    @Test
    @DisplayName("runOnce 호출 시 moveOldPartitions를 실행한다")
    void runOnceExecutesMovePartitions() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        PolicySettingsProvider provider = () -> toggles(true);
        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(ds, clock, provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "hotMonths", 0);

        job.runOnce(java.time.Instant.now());

        verify(stmt).execute(contains("ALTER TABLE audit_log_2025_04 SET TABLESPACE ts_cold"));
    }

    @Test
    @DisplayName("configureTasks - centralScheduler가 비활성화되면 로컬 스케줄을 등록한다")
    void configureTasksRegistersLocalSchedule() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "centralSchedulerEnabled", false);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");
        ReflectionTestUtils.setField(job, "cron", "0 0 5 * * 0");

        RecordingRegistrar registrar = new RecordingRegistrar();
        job.configureTasks(registrar);

        org.assertj.core.api.Assertions.assertThat(registrar.tasks).hasSize(1);
    }

    @Test
    @DisplayName("configureTasks - centralScheduler가 활성화되면 로컬 스케줄을 등록하지 않는다")
    void configureTasksSkipsWhenCentralEnabled() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "centralSchedulerEnabled", true);

        RecordingRegistrar registrar = new RecordingRegistrar();
        job.configureTasks(registrar);

        org.assertj.core.api.Assertions.assertThat(registrar.tasks).isEmpty();
    }

    @Test
    @DisplayName("currentCron - settings가 null이면 기본 cron 반환")
    void currentCronWhenSettingsNull() {
        PolicySettingsProvider provider = () -> null;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "cron", "0 0 6 * * 0");

        org.assertj.core.api.Assertions.assertThat(job.currentCron()).isEqualTo("0 0 6 * * 0");
    }

    @Test
    @DisplayName("currentCron - settings가 있지만 policyCron이 null이면 기본 cron 반환")
    void currentCronWhenPolicyCronNull() {
        // PolicyToggleSettings는 null/blank cron을 기본값으로 변환하므로 Mock 사용
        PolicyToggleSettings settings = Mockito.mock(PolicyToggleSettings.class);
        Mockito.when(settings.auditColdArchiveCron()).thenReturn(null);
        PolicySettingsProvider provider = () -> settings;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "cron", "0 0 7 * * 0");

        org.assertj.core.api.Assertions.assertThat(job.currentCron()).isEqualTo("0 0 7 * * 0");
    }

    @Test
    @DisplayName("currentCron - settings가 있고 policyCron이 blank면 기본 cron 반환")
    void currentCronWhenPolicyCronBlank() {
        // PolicyToggleSettings는 null/blank cron을 기본값으로 변환하므로 Mock 사용
        PolicyToggleSettings settings = Mockito.mock(PolicyToggleSettings.class);
        Mockito.when(settings.auditColdArchiveCron()).thenReturn("   ");
        PolicySettingsProvider provider = () -> settings;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "cron", "0 0 8 * * 0");

        org.assertj.core.api.Assertions.assertThat(job.currentCron()).isEqualTo("0 0 8 * * 0");
    }

    @Test
    @DisplayName("currentCron - cron 필드 자체가 null이면 하드코딩된 기본값 반환")
    void currentCronWhenFieldCronNull() {
        PolicySettingsProvider provider = () -> null;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "cron", null);

        org.assertj.core.api.Assertions.assertThat(job.currentCron()).isEqualTo("0 0 5 * * 0");
    }

    @Test
    @DisplayName("currentCron - cron 필드가 blank이면 하드코딩된 기본값 반환")
    void currentCronWhenFieldCronBlank() {
        PolicySettingsProvider provider = () -> null;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "cron", "   ");

        org.assertj.core.api.Assertions.assertThat(job.currentCron()).isEqualTo("0 0 5 * * 0");
    }

    @Test
    @DisplayName("isEnabled - settings가 null이면 true로 간주하고 tablespace 확인")
    void isEnabledWhenSettingsNull() {
        PolicySettingsProvider provider = () -> null;
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "coldTablespace", "ts_cold");

        org.assertj.core.api.Assertions.assertThat(job.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("isEnabled - coldTablespace가 null이면 false")
    void isEnabledWhenTablespaceNull() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditColdMaintenanceJob job = new AuditColdMaintenanceJob(mock(DataSource.class), Clock.systemUTC(), provider);
        ReflectionTestUtils.setField(job, "coldTablespace", null);

        org.assertj.core.api.Assertions.assertThat(job.isEnabled()).isFalse();
    }

    private static class RecordingRegistrar extends org.springframework.scheduling.config.ScheduledTaskRegistrar {
        java.util.List<Runnable> tasks = new java.util.ArrayList<>();

        @Override
        public void addTriggerTask(Runnable task, org.springframework.scheduling.Trigger trigger) {
            tasks.add(task);
        }
    }
}
