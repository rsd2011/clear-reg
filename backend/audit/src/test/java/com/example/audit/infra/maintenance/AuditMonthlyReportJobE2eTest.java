package com.example.audit.infra.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.audit.infra.persistence.AuditLogEntity;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "audit.monthly-report.enabled=true"
})
class AuditMonthlyReportJobE2eTest {

    @Autowired
    AuditLogRepository logRepository;

    @Autowired
    AuditMonthlySummaryRepository summaryRepository;

    @Autowired
    AuditMonthlyReportJob job;

    @Test
    @DisplayName("지난달 샘플 데이터가 집계되어 summary 테이블에 적재된다")
    void aggregatesLastMonth() {
        // given: 두 건의 지난달 로그 삽입 (성공/실패 포함)
        Instant base = LocalDate.of(2025, 2, 10).atStartOfDay().toInstant(ZoneOffset.UTC);
        Clock fixed = Clock.fixed(base, ZoneOffset.UTC);
        insertLog(fixed, base.minusSeconds(60 * 60 * 24 * 15), "LOGIN_SUCCESS", true);
        insertLog(fixed, base.minusSeconds(60 * 60 * 24 * 20), "DRM_DOWNLOAD", false);

        // when
        job.report();

        // then
        var summaries = summaryRepository.findAll();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getYearMonth()).isEqualTo("2025-01");
        assertThat(summaries.get(0).getTotalCount()).isEqualTo(2);
    }

    private void insertLog(Clock clock, Instant time, String eventType, boolean success) {
        AuditLogEntity entity = new AuditLogEntity(UUID.randomUUID(), time, eventType,
                "server", "action", "actor", "EMP", "role", "dept",
                "CUSTOMER", "123", "INTERNAL", "127.0.0.1", "UA", "DEV",
                success, "OK", "R", "text", "LB", "LOW", "b", "a", "{}", "h");
        logRepository.save(entity);
    }

    @Configuration
    static class TestConfig {
        @Bean
        PolicySettingsProvider policySettingsProvider() {
            return new PolicySettingsProvider() {
                @Override
                public PolicyToggleSettings currentSettings() {
                    return new PolicyToggleSettings(true, true, true, List.of(), 0L, List.of(), true, 0,
                            true, true, true, 0, true, "MEDIUM", true, List.of(), List.of(),
                            true, "0 0 4 1 * *", 1,
                            true, "0 0 4 1 * *");
                }

                @Override
                public com.example.common.policy.AuditPartitionSettings partitionSettings() {
                    return null;
                }
            };
        }
    }
}
