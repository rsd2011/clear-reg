package com.example.audit.infra.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditLogRetentionJobConfigTest {

    @Test
    @DisplayName("configureTasks는 Policy cron을 사용해 트리거를 등록한다")
    void configureTasksRegistersTrigger() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T03:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 3, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 15 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> toggles;
        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, provider, new AuditRetentionProperties(30));

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        job.configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(1);
        var trigger = registrar.getTriggerTaskList().get(0).getTrigger();
        var next = trigger.nextExecution(new SimpleTriggerContext());
        assertThat(next).isNotNull();
    }

    @Test
    @DisplayName("cron이 비어있으면 기본값을 사용해 스케줄한다")
    void usesDefaultCronWhenBlank() {
        AuditLogRetentionJob job = new AuditLogRetentionJob(
                Mockito.mock(com.example.audit.infra.persistence.AuditLogRepository.class),
                Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC),
                () -> new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                        true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                        false, "0 0 2 1 * *", 1,
                        true, "",
                        true, "",
                        false, "",
                        true, ""),
                new com.example.audit.config.AuditRetentionProperties(1));

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        job.configureTasks(registrar);
        assertThat(registrar.getTriggerTaskList()).hasSize(1);
    }
}
