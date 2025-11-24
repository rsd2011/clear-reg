package com.example.audit.infra;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class RetentionCleanupJobPolicyTest {

    @Test
    @DisplayName("정책 비활성화 시 정리가 실행되지 않는다")
    void skipWhenDisabledByPolicy() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        AuditRetentionProperties props = new AuditRetentionProperties(10);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 10, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                false, "0 30 3 * * *");

        RetentionCleanupJob job = new RetentionCleanupJob(repo, props, () -> toggles);
        job.purgeExpired();

        verify(repo, never()).deleteByEventTimeBefore(Mockito.any());
    }

    @Test
    @DisplayName("정책이 없으면 프로퍼티 기본값으로 실행된다")
    void fallbackWhenPolicyMissing() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        RetentionCleanupJob job = new RetentionCleanupJob(repo, new AuditRetentionProperties(3), () -> null);

        job.purgeExpired();

        verify(repo).deleteByEventTimeBefore(Mockito.any());
    }
}
