package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.Trigger;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AuditArchiveJobTest {

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
    @DisplayName("archiveCommand가 없으면 noop")
    void skipsWhenCommandMissing() {
        PolicySettingsProvider provider = () -> toggles(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "");

        job.archiveColdPartitions();

        assertThat(registry.find("audit_archive_success_total").counter().count()).isZero();
        assertThat(registry.find("audit_archive_failure_total").counter().count()).isZero();
    }

    @Test
    @DisplayName("커맨드 실행 성공 시 성공 메트릭을 증가시킨다")
    void recordsSuccessWhenCommandSucceeds() {
        PolicySettingsProvider provider = () -> toggles(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Clock clock = Clock.fixed(LocalDate.of(2025, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "echo");
        job.setInvoker((cmd, target) -> 0);

        job.archiveColdPartitions();

        assertThat(registry.find("audit_archive_success_total").counter().count()).isEqualTo(1);
        assertThat(registry.find("audit_archive_failure_total").counter().count()).isZero();
    }

    @Test
    @DisplayName("정책이 disable이면 실행하지 않는다")
    void skipsWhenPolicyDisabled() {
        PolicySettingsProvider provider = () -> toggles(false);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "echo");
        job.setInvoker((cmd, target) -> { throw new AssertionError("should not run"); });

        job.archiveColdPartitions();

        assertThat(registry.find("audit_archive_success_total").counter().count()).isZero();
        assertThat(registry.find("audit_archive_failure_total").counter().count()).isZero();
    }

    @Test
    @DisplayName("커맨드가 실패하면 실패 메트릭을 증가시킨다")
    void recordsFailureWhenCommandFails() {
        PolicySettingsProvider provider = () -> toggles(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "echo");
        ReflectionTestUtils.setField(job, "retry", 1);
        job.setInvoker((cmd, target) -> 99);

        job.archiveColdPartitions();

        assertThat(registry.find("audit_archive_failure_total").counter().count()).isEqualTo(1);
        assertThat(registry.find("audit_archive_success_total").counter().count()).isZero();
    }

    @Test
    @DisplayName("실패 시 슬랙 알림을 전송한다")
    void sendsSlackOnFailure() {
        PolicySettingsProvider provider = () -> toggles(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "echo");
        ReflectionTestUtils.setField(job, "retry", 1);
        ReflectionTestUtils.setField(job, "slackWebhook", "http://localhost");
        ReflectionTestUtils.setField(job, "alertEnabled", true);
        org.springframework.web.client.RestTemplate restTemplate = org.mockito.Mockito.mock(org.springframework.web.client.RestTemplate.class);
        job.setRestTemplate(restTemplate);
        job.setInvoker((cmd, target) -> 2);

        job.archiveColdPartitions();

        org.mockito.Mockito.verify(restTemplate).postForEntity(org.mockito.Mockito.eq("http://localhost"),
                org.mockito.Mockito.contains("FAILED"), org.mockito.Mockito.eq(String.class));
        assertThat(registry.find("audit_archive_failure_total").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("지연 알림 기준을 넘기면 슬랙 지연 메시지를 전송한다")
    void sendsSlackOnDelay() {
        PolicySettingsProvider provider = () -> toggles(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), registry, provider);
        ReflectionTestUtils.setField(job, "archiveCommand", "echo");
        ReflectionTestUtils.setField(job, "retry", 1);
        ReflectionTestUtils.setField(job, "slackWebhook", "http://localhost");
        ReflectionTestUtils.setField(job, "alertEnabled", true);
        ReflectionTestUtils.setField(job, "delayThresholdMs", 0L);
        org.springframework.web.client.RestTemplate restTemplate = org.mockito.Mockito.mock(org.springframework.web.client.RestTemplate.class);
        job.setRestTemplate(restTemplate);
        job.setInvoker((cmd, target) -> 0);

        job.archiveColdPartitions();

        org.mockito.Mockito.verify(restTemplate).postForEntity(org.mockito.Mockito.eq("http://localhost"),
                org.mockito.Mockito.contains("SLOW"), org.mockito.Mockito.eq(String.class));
        assertThat(registry.find("audit_archive_success_total").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("정책 변경 이벤트를 안전하게 처리한다")
    void handlesPolicyChangedEvent() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), new SimpleMeterRegistry(), provider);
        job.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("security.policy", "yaml"));
        job.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("other", "yaml"));
    }

    @Test
    @DisplayName("batchJobSchedule이 있으면 trigger는 정책 스케줄을 사용한다")
    void triggerUsesPolicySchedule() {
        PolicySettingsProvider provider = org.mockito.Mockito.mock(PolicySettingsProvider.class);
        org.mockito.Mockito.when(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_ARCHIVE))
                .thenReturn(new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.CRON, "0 0 1 * * *", 0, 0, null));
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), new SimpleMeterRegistry(), provider);

        org.assertj.core.api.Assertions.assertThat(job.trigger().expression()).isEqualTo("0 0 1 * * *");
    }

    @Test
    @DisplayName("central scheduler가 활성화되면 로컬 스케줄 등록을 생략한다")
    void skipsLocalSchedulingWhenCentralEnabled() {
        PolicySettingsProvider provider = () -> toggles(true);
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC(), new SimpleMeterRegistry(), provider);
        ReflectionTestUtils.setField(job, "centralSchedulerEnabled", true);

        RecordingRegistrar registrar = new RecordingRegistrar();

        job.configureTasks(registrar);

        org.assertj.core.api.Assertions.assertThat(registrar.tasks).isEmpty();
    }

    private static class RecordingRegistrar extends ScheduledTaskRegistrar {
        java.util.List<Runnable> tasks = new java.util.ArrayList<>();

        @Override
        public void addTriggerTask(Runnable task, Trigger trigger) {
            tasks.add(task);
        }
    }
}
