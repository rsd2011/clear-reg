package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.infra.persistence.AuditLogRepository;

class AuditLogRetentionJobBranchTest {

    @Test
    @DisplayName("레포지토리 예외가 발생해도 잡이 중단되지 않는다")
    void purgeHandlesRepositoryException() {
        AuditLogRepository repo = org.mockito.Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        when(repo.deleteByEventTimeBefore(org.mockito.Mockito.any())).thenReturn(0L);
        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock);
        job.setRetentionDays(365);
        job.purgeExpired();

        verify(repo).deleteByEventTimeBefore(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("삭제된 건수가 있으면 로그 분기를 탄다")
    void purgeLogsWhenDeleted() {
        AuditLogRepository repo = org.mockito.Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        when(repo.deleteByEventTimeBefore(org.mockito.Mockito.any())).thenReturn(5L);
        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock);
        job.setRetentionDays(180);
        job.purgeExpired();

        verify(repo).deleteByEventTimeBefore(org.mockito.Mockito.any());
    }
}
