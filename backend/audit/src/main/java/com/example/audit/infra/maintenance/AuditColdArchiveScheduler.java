package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HOT→COLD 이동 및 Object Lock/Glacier 배치를 트리거하는 스켈레톤.
 * 실제 스토리지 작업은 별도 배치/스크립트와 연동하도록 확장한다.
 */
@Component
public class AuditColdArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditColdArchiveScheduler.class);

    private final Clock clock;
    private final boolean enabled;

    public AuditColdArchiveScheduler(Clock clock,
                                     @org.springframework.beans.factory.annotation.Value("${audit.archive.enabled:false}") boolean enabled) {
        this.clock = clock;
        this.enabled = enabled;
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
        LocalDate coldTarget = today.minusMonths(7).withDayOfMonth(1);
        log.info("[audit-archive] prepare move to COLD for partition month {}", coldTarget);
        // TODO: invoke actual archive job (Object Lock/Glacier) via command bus or batch launcher
    }
}
