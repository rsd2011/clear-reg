package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class RetentionCleanupJobConfigTest {

    @Test
    @DisplayName("configureTasks는 Policy cron을 사용해 등록된다")
    void configureTasksRegistersTrigger() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 45 3 * * *");
        RetentionCleanupJob job = new RetentionCleanupJob(repo, new AuditRetentionProperties(15), () -> toggles);

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        job.configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(1);
        var trigger = registrar.getTriggerTaskList().get(0).getTrigger();
        assertThat(trigger.nextExecution(new SimpleTriggerContext())).isNotNull();
    }

    @Test
    @DisplayName("정책 cron이 비어있으면 기본값을 사용한다")
    void defaultsWhenCronBlank() {
        RetentionCleanupJob job = new RetentionCleanupJob(Mockito.mock(AuditLogRepository.class), new AuditRetentionProperties(5),
                () -> new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                        true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                        false, "0 0 2 1 * *", 1,
                        true, "0 0 4 1 * *",
                        true, "0 0 3 * * *",
                        false, "0 30 2 2 * *",
                        true, ""));

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        job.configureTasks(registrar);
        assertThat(registrar.getTriggerTaskList()).hasSize(1);
    }
}
