package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditMonthlyReportJobBranchTest {

    @Test
    @DisplayName("집계 중 예외가 발생해도 잡이 중단되지 않는다")
    void aggregateHandlesException() {
        AuditLogRepository logRepo = org.mockito.Mockito.mock(AuditLogRepository.class);
        AuditMonthlySummaryRepository repo = org.mockito.Mockito.mock(AuditMonthlySummaryRepository.class);
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        PolicyToggleSettings disabled = new PolicyToggleSettings(true, true, true, List.of(), 20971520L, List.of(), true,
                365, true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(), false,
                "0 0 2 1 * *", 1, false, "0 0 4 1 * *");
        PolicySettingsProvider provider = () -> disabled;

        AuditMonthlyReportJob job = new AuditMonthlyReportJob(logRepo, repo, clock, provider);
        job.report(); // 비활성화 분기 커버

        verify(repo, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
    }
}
