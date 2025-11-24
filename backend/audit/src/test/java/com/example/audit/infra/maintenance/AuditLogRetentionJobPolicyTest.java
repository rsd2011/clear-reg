package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.never;
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

class AuditLogRetentionJobPolicyTest {

    @Test
    @DisplayName("정책으로 비활성화되면 삭제를 수행하지 않는다")
    void skipWhenPolicyDisabled() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings disabled = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                false, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> disabled;

        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, provider, new AuditRetentionProperties(30));
        job.purgeExpired();

        verify(repo, never()).deleteByEventTimeBefore(Mockito.any());
    }

    @Test
    @DisplayName("정책의 보존일수가 적용된다")
    void usesPolicyRetentionDays() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T03:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings enabled = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 5, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");

        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, () -> enabled, new AuditRetentionProperties(30));

        job.purgeExpired();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repo).deleteByEventTimeBefore(captor.capture());
        Instant threshold = captor.getValue();
        // 5일 이전으로 계산되었는지 확인
        Instant expected = clock.instant().minus(java.time.Duration.ofDays(5));
        org.assertj.core.api.Assertions.assertThat(threshold).isEqualTo(expected);
    }

    @Test
    @DisplayName("정책이 없을 때는 기본 설정으로 동작한다")
    void fallsBackWhenPolicyMissing() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T03:00:00Z"), ZoneOffset.UTC);
        AuditLogRetentionJob job = new AuditLogRetentionJob(repo, clock, () -> null, new AuditRetentionProperties(2));

        job.purgeExpired();

        verify(repo).deleteByEventTimeBefore(Mockito.any());
    }
}
