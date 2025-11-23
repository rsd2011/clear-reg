package com.example.audit.infra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;

@DisplayName("RetentionCleanupJob 분기 커버")
class RetentionCleanupJobTest {

    @Test
    void purgeLogsCallsRepository() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        when(repo.deleteByEventTimeBefore(any())).thenReturn(0L); // deleted == 0 브랜치
        AuditRetentionProperties props = new AuditRetentionProperties(30);
        RetentionCleanupJob job = new RetentionCleanupJob(repo, props);
        job.purgeExpired();

        when(repo.deleteByEventTimeBefore(any())).thenReturn(5L); // deleted > 0 브랜치
        job.purgeExpired();
        verify(repo, Mockito.times(2)).deleteByEventTimeBefore(any());
    }
}
