package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicySettingsProvider;

/**
 * HOT→COLD 이동 및 Object Lock/Glacier 배치를 트리거하는 스켈레톤.
 * 실제 스토리지 작업은 별도 배치/스크립트와 연동하도록 확장한다.
 */
@Component
public class AuditColdArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditColdArchiveScheduler.class);

    private final Clock clock;
    private final boolean enabled;
    private final PolicySettingsProvider policySettingsProvider;
    private final String archiveCommand;
    private final int hotMonths;

    public AuditColdArchiveScheduler(Clock clock,
                                     PolicySettingsProvider policySettingsProvider,
                                     @Value("${audit.archive.enabled:false}") boolean enabled,
                                     @Value("${audit.archive.command:}") String archiveCommand,
                                     @Value("${audit.partition.hot-months:6}") int hotMonths) {
        this.clock = clock;
        this.policySettingsProvider = policySettingsProvider;
        this.enabled = enabled;
        this.archiveCommand = archiveCommand;
        this.hotMonths = hotMonths <= 0 ? 6 : hotMonths;
    }

    /**
     * 매월 2일 02:30 실행 (예: HOT→COLD 이동 대상 파티션 목록 계산 후 외부 배치 호출)
     */
    @Scheduled(cron = "0 30 2 2 * *")
    public void scheduleArchive() {
        if (!enabled) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        AuditPartitionSettings ps = policySettingsProvider.partitionSettings();
        int hotWindow = ps != null ? ps.hotMonths() : hotMonths;
        LocalDate coldTarget = today.minusMonths(hotWindow + 1).withDayOfMonth(1);
        log.info("[audit-archive] prepare move to COLD for partition month {} (hotWindow={}, command={})",
                coldTarget, hotWindow, archiveCommand.isBlank() ? "(noop)" : archiveCommand);
        // TODO: archiveCommand 연동(프로세스 실행 또는 배치 런처) + 실패 시 알림/리트라이
    }

    /** 정책 변경 시 즉시 설정 반영을 위한 훅 (정책 이벤트가 출판될 경우) */
    @EventListener
    public void onPolicyChanged(Object event) {
        if (!enabled) {
            return;
        }
        log.info("[audit-archive] policy changed, next archive target will refresh on next cron");
        scheduleArchive();
    }
}
