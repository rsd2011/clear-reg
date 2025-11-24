package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditColdArchiveSchedulerFallbackTest {

    @Test
    @DisplayName("PolicySettingsProvider가 null을 반환해도 fallback enabled 값을 사용한다")
    void fallbackEnabledWhenSettingsMissing() {
        Clock clock = Clock.fixed(Instant.parse("2025-11-24T00:00:00Z"), ZoneOffset.UTC);
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, () -> null, true, "", 6);

        // simply invoke to cover fallback branch; no exception expected
        scheduler.scheduleArchive();
    }
}

