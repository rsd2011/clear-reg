package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.infra.persistence.AuditLogRepository;

class AuditLogRetentionJobTest {

    @Test
    @DisplayName("retention 기간 이전 레코드를 삭제한다")
    void purgeExpiredDeletesOld() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, () -> null,
                new com.example.audit.config.AuditRetentionProperties(30));
        job.setRetentionDays(30);

        job.purgeExpired();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repo).deleteByEventTimeBefore(captor.capture());
        // 2025-01-01 - 30d = 2024-12-02
        assert captor.getValue().isBefore(Instant.parse("2024-12-03T00:00:00Z"));
    }
}
