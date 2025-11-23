package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.infra.persistence.AuditLogRepository;

class AuditMonthlyReportJobTest {

    @Test
    @DisplayName("지난달 건수 집계를 호출한다")
    void reportInvokesCount() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditMonthlyReportJob job = new AuditMonthlyReportJob(repo, clock);

        job.report();

        verify(repo).count();
    }
}
