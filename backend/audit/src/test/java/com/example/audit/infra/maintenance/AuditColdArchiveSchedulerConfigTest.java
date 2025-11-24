package com.example.audit.infra.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditColdArchiveSchedulerConfigTest {

    @Test
    @DisplayName("configureTasks는 정책 cron을 사용해 트리거를 등록한다")
    void configureTasksUsesPolicyCron() {
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                true, "0 15 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> toggles;
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, provider, true, "/bin/echo", 6);

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        scheduler.configureTasks(registrar);

        var tasks = registrar.getTriggerTaskList();
        assertThat(tasks).hasSize(1);
        var trigger = tasks.get(0).getTrigger();
        var next = trigger.nextExecution(new SimpleTriggerContext());
        assertThat(next).isNotNull();
    }
}

