package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditArchiveJobTest {

    @Test
    @DisplayName("enabled=false면 실행을 건너뛴다")
    void skipWhenDisabled() {
        AuditArchiveJob job = new AuditArchiveJob(Clock.systemUTC());
        setEnabled(job, false);
        assertThatCode(job::archiveColdPartitions).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("enabled=true면 대상 월을 계산한다")
    void runWhenEnabled() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock);
        setEnabled(job, true);
        assertThatCode(job::archiveColdPartitions).doesNotThrowAnyException();
    }

    private void setEnabled(AuditArchiveJob job, boolean value) {
        try {
            var f = AuditArchiveJob.class.getDeclaredField("enabled");
            f.setAccessible(true);
            f.set(job, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
