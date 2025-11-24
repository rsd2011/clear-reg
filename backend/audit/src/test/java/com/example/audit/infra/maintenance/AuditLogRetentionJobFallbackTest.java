package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditLogRetentionJobFallbackTest {

    @Test
    @DisplayName("정책의 보존일수가 0 이하이면 프로퍼티 기본값으로 보정한다")
    void usesPropertyWhenPolicyInvalid() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings invalid = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 0, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> invalid;

        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, provider, new AuditRetentionProperties(7));
        job.purgeExpired();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repo).deleteByEventTimeBefore(captor.capture());
        // 정책 days가 0이면 retentionDays 기본값(1095일)로 보정된다.
        Instant expected = clock.instant().minus(java.time.Duration.ofDays(1095));
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).isEqualTo(expected);
    }
}
