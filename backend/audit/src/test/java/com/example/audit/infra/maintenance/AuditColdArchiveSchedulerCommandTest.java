package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditColdArchiveSchedulerCommandTest {

    @Test
    @DisplayName("archiveCommand가 있을 때도 스케줄이 실행된다")
    void scheduleArchiveWithCommand() {
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC);
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true, java.util.List.of(), 0L, java.util.List.of(), true, 30,
                true, true, true, 30, true, "MEDIUM", true, java.util.List.of(), java.util.List.of(),
                false, "0 0 2 1 * *", 1,
                true, "0 0 4 1 * *",
                true, "0 30 2 2 * *",
                true, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        PolicySettingsProvider provider = () -> toggles;
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, provider, true, "/bin/echo", 6);

        // 단순 실행으로 archiveCommand 설정 분기 커버
        scheduler.scheduleArchive();
    }
}

