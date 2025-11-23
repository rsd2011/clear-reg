package com.example.audit.infra;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;

/**
 * 보존기간이 지난 감사 로그를 주기적으로 정리하는 잡.
 * 파티션 사용 시 파티션 DROP 배치로 대체 가능.
 */
@Component
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);

    private final AuditLogRepository repository;
    private final AuditRetentionProperties properties;

    public RetentionCleanupJob(AuditLogRepository repository, AuditRetentionProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 30 3 * * *") // 매일 03:30
    public void purgeExpired() {
        int days = properties.days();
        Instant threshold = Instant.now().minus(days, ChronoUnit.DAYS);
        long deleted = repository.deleteByEventTimeBefore(threshold);
        if (deleted > 0) {
            log.info("audit retention purge deleted={} before={}days", deleted, days);
        }
    }
}
