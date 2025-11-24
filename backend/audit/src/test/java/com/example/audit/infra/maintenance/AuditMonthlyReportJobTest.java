package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.persistence.AuditMonthlySummaryEntity;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;

class AuditMonthlyReportJobTest {

    @Test
    @DisplayName("지난달 건수 집계를 호출한다")
    void reportInvokesCount() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        AuditMonthlySummaryRepository summaryRepo = Mockito.mock(AuditMonthlySummaryRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        var policy = Mockito.mock(com.example.common.policy.PolicySettingsProvider.class);
        Mockito.when(policy.currentSettings()).thenReturn(defaultSettings());
        AuditMonthlyReportJob job = new AuditMonthlyReportJob(repo, summaryRepo, clock, policy);

        job.report();

        Instant from = LocalDate.of(2025, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2025, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        verify(repo).countByEventTimeBetween(from, to);
        verify(repo).countByEventTimeBetweenAndSuccess(from, to, false);
        verify(repo).countByEventTimeBetweenAndEventTypeIn(from, to, List.of("UNMASK"));
        verify(summaryRepo).save(Mockito.argThat(e ->
                e.getYearMonth().equals("2025-01") &&
                        e.getTotalCount() == 0L));
    }

    @Test
    @DisplayName("월간 요약 엔티티 빌더 기본 동작")
    void summaryEntityBuilder() {
        Instant now = Instant.parse("2025-01-02T00:00:00Z");
        AuditMonthlySummaryEntity ent = AuditMonthlySummaryEntity.builder()
                .yearMonth("2025-01")
                .totalCount(123L)
                .createdAt(now)
                .build();

        org.assertj.core.api.Assertions.assertThat(ent.getYearMonth()).isEqualTo("2025-01");
        org.assertj.core.api.Assertions.assertThat(ent.getTotalCount()).isEqualTo(123L);
        org.assertj.core.api.Assertions.assertThat(ent.getCreatedAt()).isEqualTo(now);
    }

    private com.example.common.policy.PolicyToggleSettings defaultSettings() {
        return new com.example.common.policy.PolicyToggleSettings(true, true, true, List.of(), 0L, List.of(), true, 0,
                true, true, true, 0, true, "MEDIUM", true, List.of(), List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
    }
}
