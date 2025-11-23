package com.example.batch.audit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HOT→COLD 이동 이후 Object Lock/Glacier 전송을 외부 스크립트/배치로 호출하기 위한 훅.
 * 현재는 로그만 남기며, 향후 Shell/BatchLauncher 연동 시 이 클래스를 확장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditArchiveJob {

    private final Clock clock;

    @Value("${audit.archive.enabled:false}")
    private boolean enabled;

    @Value("${audit.archive.cron:0 30 3 2 * *}")
    private String cron;

    @Value("${audit.archive.command:}")
    private String archiveCommand;

    @Value("${audit.archive.retry:3}")
    private int retry;

    @Scheduled(cron = "${audit.archive.cron:0 30 3 2 * *}")
    public void archiveColdPartitions() {
        if (!enabled) {
            return;
        }
        LocalDate target = LocalDate.now(clock).minusMonths(7).withDayOfMonth(1);
        log.info("[audit-archive] trigger archive for {} (cron={}, cmd={}, retry={})", target, cron, archiveCommand, retry);
        if (!StringUtils.hasText(archiveCommand)) {
            return; // noop if command not provided
        }
        // TODO: invoke external script with target and retry (e.g., ProcessBuilder + exit code handling + Slack alert)
    }
}
