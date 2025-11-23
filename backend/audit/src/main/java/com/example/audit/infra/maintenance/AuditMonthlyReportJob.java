package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.persistence.AuditMonthlySummaryEntity;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

/**
 * 월간 접속/감사 로그 점검 리포트를 생성하기 위한 스켈레톤.
 * 현재는 건수만 수집해 로그로 남기며, 향후 SIEM/Grafana 연계 시 데이터를 내보내도록 확장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class AuditMonthlyReportJob implements org.springframework.scheduling.annotation.SchedulingConfigurer {

    private final AuditLogRepository repository;
    private final AuditMonthlySummaryRepository summaryRepository;
    private final Clock clock;
    private final PolicySettingsProvider policySettingsProvider;

    /**
     * 매월 1일 04:00에 지난달 로그 건수 리포트.
     */
    public void report() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        if (!settings.auditMonthlyReportEnabled()) {
            return;
        }
        LocalDate now = LocalDate.now(clock);
        LocalDate start = now.minusMonths(1).withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(1);
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = end.atStartOfDay().toInstant(ZoneOffset.UTC);

        long count = repository.countByEventTimeBetween(from, to);
        long failureCount = repository.countByEventTimeBetweenAndSuccess(from, to, false);
        long unmaskCount = repository.countByEventTimeBetweenAndEventTypeIn(from, to, List.of("UNMASK"));
        long drmDownloadCount = repository.countByEventTimeBetweenAndEventTypeIn(from, to, List.of("DRM_DOWNLOAD", "DRM_EXPORT", "DOWNLOAD"));

        log.info("Audit monthly report {} ~ {} count={}", start, end.minusDays(1), count);
        log.info("Audit monthly metrics: failures={}, unmaskRequests={}, drm/download={}", failureCount, unmaskCount, drmDownloadCount);

        String yearMonth = start.toString().substring(0, 7); // yyyy-MM
        summaryRepository.save(AuditMonthlySummaryEntity.builder()
                .yearMonth(yearMonth)
                .totalCount(count)
                .createdAt(Instant.now(clock))
                .build());
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::report, new Trigger() {
            @Override
            public java.util.Date nextExecutionTime(TriggerContext triggerContext) {
                String cron = policySettingsProvider.currentSettings().auditMonthlyReportCron();
                return new CronTrigger(cron).nextExecutionTime(triggerContext);
            }
        });
    }
}
