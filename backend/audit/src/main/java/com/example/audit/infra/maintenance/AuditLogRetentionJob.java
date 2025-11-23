package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.audit.infra.persistence.AuditLogRepository;

/**
 * 보존 기간을 초과한 감사 로그를 주기적으로 정리하는 잡.
 * 정책 연동 시 retentionDays 값을 외부 설정으로부터 주입받도록 확장할 수 있다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRetentionJob {

    private final AuditLogRepository repository;
    private final Clock clock;
    /**
     * 기본 3년(1095일) 보존. 정책/설정으로 주입 가능.
     */
    private long retentionDays = 1095;

    /**
     * 매일 새벽 3시에 만료 데이터 삭제.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        Instant threshold = clock.instant().minus(Duration.ofDays(retentionDays));
        long deleted = repository.deleteByEventTimeBefore(threshold);
        if (deleted > 0) {
            log.info("Audit retention purge deleted {} rows older than {}", deleted, threshold);
        }
    }

    public void setRetentionDays(long retentionDays) {
        this.retentionDays = retentionDays;
    }
}
