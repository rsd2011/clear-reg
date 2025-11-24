package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditColdArchiveSchedulerCoverageTest {

    @Test
    @DisplayName("정책 이벤트 수신 시 scheduleArchive가 호출된다")
    void onPolicyChangeTriggersSchedule() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        com.example.common.policy.PolicyToggleSettings settings = new com.example.common.policy.PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                true, true, true, 0, true, "MEDIUM", true, null, null,
                true, "0 0 2 1 * *", 1, true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *");
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, () -> settings, true, "", 6);
        scheduler.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("security.policy", "yaml"));
    }

    @Test
    @DisplayName("다른 코드의 정책 이벤트는 무시된다")
    void ignoresDifferentPolicyCode() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, () -> null, true, "", 6);
        scheduler.onPolicyChanged(new com.example.common.policy.PolicyChangedEvent("other", ""));
    }
}
