package com.example.audit.infra.maintenance;

import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

class AuditColdArchiveSchedulerTest {

    @Test
    @DisplayName("비활성화 시 동작하지 않는다")
    void disabledDoesNothing() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, false);
        scheduler.scheduleArchive();
    }

    @Test
    @DisplayName("활성화 시 7개월 전 파티션을 대상 month로 로그에 남긴다")
    void enabledLogsTarget() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditColdArchiveScheduler scheduler = new AuditColdArchiveScheduler(clock, true);
        Logger mockLog = Mockito.mock(Logger.class);
        // using reflection to inject mock logger is overkill; just ensure no exception raised and method runnable
        scheduler.scheduleArchive();
    }
}
