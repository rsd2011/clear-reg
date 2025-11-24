package com.example.audit.infra.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditMonthlyReportJobConfigTest {

    @Test
    @DisplayName("configureTasks는 정책 cron으로 트리거를 등록한다")
    void configureTasksRegistersCronTrigger() {
        AuditLogRepository logRepo = Mockito.mock(AuditLogRepository.class);
        AuditMonthlySummaryRepository summaryRepo = Mockito.mock(AuditMonthlySummaryRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 0,
                true, true, true, 0, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 5 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> toggles;

        AuditMonthlyReportJob job = new AuditMonthlyReportJob(logRepo, summaryRepo, clock, provider);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        job.configureTasks(registrar);

        var trigger = registrar.getTriggerTaskList().get(0).getTrigger();
        assertThat(trigger.nextExecution(new SimpleTriggerContext())).isNotNull();
    }
}

