package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.AuditPartitionSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

class AuditColdArchiveSchedulerTest {

    @Test
    @DisplayName("비활성화 시 동작하지 않는다")
    void disabledDoesNothing() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, () -> null, false, "", 6);
        scheduler.scheduleArchive();
    }

    @Test
    @DisplayName("활성화 시 7개월 전 파티션을 대상 month로 로그에 남긴다")
    void enabledLogsTarget() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditPartitionSettings partitionSettings = new AuditPartitionSettings(true, "0 0 2 2 * *", 2, "audit_hot", "audit_cold", 3, 60);
        var provider = new com.example.common.policy.PolicySettingsProvider() {
            @Override
            public com.example.common.policy.PolicyToggleSettings currentSettings() {
                return new com.example.common.policy.PolicyToggleSettings(true, true, true, java.util.List.of(), 20971520L, java.util.List.of(), true,
                        365, true, true, true, 730, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(), false,
                        "0 0 2 1 * *", 1, true, "0 0 4 1 * *");
            }

            @Override
            public AuditPartitionSettings partitionSettings() {
                return partitionSettings;
            }
        };
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, provider, true, "/bin/echo", 6);
        scheduler.scheduleArchive();
    }
}
