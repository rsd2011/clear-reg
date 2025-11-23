package com.example.batch.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

/**
 * PostgreSQL 기준 월 단위 파티션을 사전 생성하는 배치 스케줄러.
 * 정책/프로퍼티로 enable/cron/preloadMonths를 조정할 수 있다.
 */
@Component
@Slf4j
public class AuditPartitionScheduler implements SchedulingConfigurer {

    private final DataSource dataSource;
    private final Clock clock;
    private final PolicySettingsProvider policySettingsProvider;
    private final MeterRegistry meterRegistry;

    @Value("${audit.partition.enabled:false}")
    private boolean enabledFallback;
    @Value("${audit.partition.cron:0 0 2 1 * *}")
    private String cronFallback;
    @Value("${audit.partition.preload-months:1}")
    private int preloadMonthsFallback;
    @Value("${audit.partition.tablespace.hot:}")
    private String hotTablespace;
    @Value("${audit.partition.tablespace.cold:}")
    private String coldTablespace;
    @Value("${audit.partition.hot-months:6}")
    private int hotMonths;
    @Value("${audit.partition.cold-months:60}")
    private int coldMonths;

    private final Counter partitionSuccess;
    private final Counter partitionFailure;
    private final Timer partitionLatency;

    public AuditPartitionScheduler(DataSource dataSource,
                                   Clock clock,
                                   PolicySettingsProvider policySettingsProvider,
                                   MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.clock = clock;
        this.policySettingsProvider = policySettingsProvider;
        this.meterRegistry = meterRegistry;
        this.partitionSuccess = meterRegistry.counter("audit_partition_create_success_total");
        this.partitionFailure = meterRegistry.counter("audit_partition_create_failure_total");
        this.partitionLatency = Timer.builder("audit_partition_create_ms")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::createNextPartitions, triggerContext -> {
            String cron = currentSettings().auditPartitionCron();
            return new CronTrigger(cron).nextExecution(triggerContext);
        });
    }

    void createNextPartitions() {
        PolicyToggleSettings settings = currentSettings();
        AuditPartitionSettings ps = policySettingsProvider.partitionSettings();
        boolean enabled = ps != null ? ps.enabled() : settings.auditPartitionEnabled();
        if (!enabled) {
            return;
        }
        int months = Math.max(ps != null ? ps.preloadMonths() : settings.auditPartitionPreloadMonths(), 0);
        LocalDate start = LocalDate.now(clock).plusMonths(1).withDayOfMonth(1);
        String tsHot = (ps != null && !ps.tablespaceHot().isBlank()) ? ps.tablespaceHot() : hotTablespace;
        String tsCold = (ps != null && !ps.tablespaceCold().isBlank()) ? ps.tablespaceCold() : coldTablespace;
        int hotWindow = ps != null ? ps.hotMonths() : hotMonths;
        int coldWindow = ps != null ? ps.coldMonths() : coldMonths;

        IntStream.rangeClosed(0, months)
                .forEach(offset -> ensurePartition(start.plusMonths(offset), tsHot));

        // HOT→COLD 이동 대상 파티션 힌트 로그 (실제 이동은 AuditArchiveJob에서 수행)
        LocalDate coldTarget = LocalDate.now(clock).minusMonths(hotWindow + 1).withDayOfMonth(1);
        log.debug("[audit-partition] hotWindow={} coldWindow={} tablespaceHot={} tablespaceCold={} nextColdTarget={}",
                hotWindow, coldWindow, tsHot, tsCold, coldTarget);
    }

    @EventListener
    public void refreshOnPolicyChange(com.example.common.policy.PolicyChangedEvent event) {
        if ("security.policy".equals(event.code())) {
            log.info("[audit-partition] policy changed, refreshing partition creation");
            createNextPartitions();
        }
    }

    void ensurePartition(LocalDate monthStart, String tsHot) {
        String suffix = monthStart.format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        String tablespace = tsHot != null && !tsHot.isBlank() ? " TABLESPACE " + tsHot : "";
        String ddl = """
                CREATE TABLE IF NOT EXISTS %s PARTITION OF audit_log
                FOR VALUES FROM ('%s') TO ('%s')%s;
                """.formatted(partitionName, monthStart, monthStart.plusMonths(1), tablespace);
        long started = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(ddl);
            log.info("Audit partition ensured: {}", partitionName);
            partitionSuccess.increment();
        } catch (SQLException e) {
            log.warn("Failed to create audit partition {}: {}", partitionName, e.getMessage());
            partitionFailure.increment();
        } finally {
            long elapsed = System.currentTimeMillis() - started;
            partitionLatency.record(elapsed, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private PolicyToggleSettings currentSettings() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        if (settings == null) {
            return new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                    true, true, true, 0, true, "MEDIUM", true, null, null,
                    enabledFallback, cronFallback, preloadMonthsFallback,
                    true, "0 0 4 1 * *");
        }
        AuditPartitionSettings ps = policySettingsProvider.partitionSettings();
        boolean enabled = ps != null ? ps.enabled() : settings.auditPartitionEnabled();
        String cron = ps != null ? ps.cron()
                : (settings.auditPartitionCron() == null || settings.auditPartitionCron().isBlank()
                ? cronFallback : settings.auditPartitionCron());
        int preload = ps != null ? ps.preloadMonths()
                : (settings.auditPartitionPreloadMonths() < 0 ? preloadMonthsFallback : settings.auditPartitionPreloadMonths());
        return new PolicyToggleSettings(settings.passwordPolicyEnabled(), settings.passwordHistoryEnabled(), settings.accountLockEnabled(),
                settings.enabledLoginTypes(), settings.maxFileSizeBytes(), settings.allowedFileExtensions(), settings.strictMimeValidation(),
                settings.fileRetentionDays(), settings.auditEnabled(), settings.auditReasonRequired(), settings.auditSensitiveApiDefaultOn(),
                settings.auditRetentionDays(), settings.auditStrictMode(), settings.auditRiskLevel(), settings.auditMaskingEnabled(),
                settings.auditSensitiveEndpoints(), settings.auditUnmaskRoles(),
                enabled, cron, preload, settings.auditMonthlyReportEnabled(), settings.auditMonthlyReportCron());
    }
}
