package com.example.audit.infra.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.common.policy.PolicySettingsProvider;

class AuditSchedulerPortTest {

    private com.example.common.schedule.BatchJobSchedule schedule(String cron) {
        return new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.CRON, cron, 0, 0, null);
    }

    @Test
    @DisplayName("AuditLogRetentionJob은 정책 스케줄을 우선 사용한다")
    void retentionTriggerUsesPolicySchedule() {
        var provider = mock(PolicySettingsProvider.class);
        org.mockito.Mockito.when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_LOG_RETENTION))
                .thenReturn(schedule("0 0 1 * * *"));
        var repo = mock(com.example.audit.infra.persistence.AuditLogRepository.class);
        var props = mock(com.example.audit.config.AuditRetentionProperties.class);
        org.mockito.Mockito.when(props.days()).thenReturn(30);

        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, Clock.systemUTC(), provider, props);

        assertThat(job.trigger().expression()).isEqualTo("0 0 1 * * *");
        job.runOnce(Instant.now()); // should not throw
    }

    @Test
    @DisplayName("AuditColdArchiveScheduler도 정책 스케줄을 우선 사용한다")
    void coldArchiveUsesPolicy() {
        var provider = mock(PolicySettingsProvider.class);
        org.mockito.Mockito.when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_COLD_ARCHIVE_SCHEDULER))
                .thenReturn(schedule("0 15 2 2 * *"));

        AuditColdArchiveScheduler job = new AuditColdArchiveScheduler(Clock.systemUTC(), provider, true, "", 6);

        assertThat(job.trigger().expression()).isEqualTo("0 15 2 2 * *");
    }

    @Test
    @DisplayName("AuditMonthlyReportJob은 central scheduler가 켜지면 로컬 등록을 생략한다")
    void monthlyReportSkipsLocalWhenCentral() {
        var provider = mock(PolicySettingsProvider.class);
        org.mockito.Mockito.when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT))
                .thenReturn(schedule("0 0 4 1 * *"));
        var repo = mock(com.example.audit.infra.persistence.AuditLogRepository.class);
        var summaryRepo = mock(com.example.audit.infra.persistence.AuditMonthlySummaryRepository.class);

        AuditMonthlyReportJob job = new AuditMonthlyReportJob(repo, summaryRepo, Clock.fixed(Instant.now(), ZoneOffset.UTC), provider);
        ReflectionTestUtils.setField(job, "centralSchedulerEnabled", true);

        RecordingRegistrar registrar = new RecordingRegistrar();
        job.configureTasks(registrar);

        assertThat(registrar.count).isZero();
    }

    private static class RecordingRegistrar extends ScheduledTaskRegistrar {
        int count = 0;
        @Override
        public void addTriggerTask(Runnable task, org.springframework.scheduling.Trigger trigger) {
            count++;
        }
    }
}
