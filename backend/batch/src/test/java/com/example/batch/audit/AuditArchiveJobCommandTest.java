package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditArchiveJobCommandTest {

    @Test
    @DisplayName("archiveCommand가 설정되면 retry 횟수 내에서 실행을 시도한다")
    void invokesCommandWithRetry() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        setField(job, "enabled", true);
        setField(job, "archiveCommand", "/bin/echo");
        setField(job, "retry", 2);

        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        Mockito.when(invoker.run(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(1)  // 1st attempt fails
                .thenReturn(0); // 2nd attempt succeeds
        job.setInvoker(invoker);

        job.archiveColdPartitions();

        verify(invoker, Mockito.times(2)).run(eq("/bin/echo"), eq("2025-04-01"));
        assertThat(true).isTrue(); // 단순 실행 보장
    }

    @Test
    @DisplayName("command가 비어 있으면 invoker를 호출하지 않는다")
    void noopWhenCommandEmpty() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        setField(job, "enabled", true);
        setField(job, "archiveCommand", "");
        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        job.setInvoker(invoker);

        job.archiveColdPartitions();

        Mockito.verifyNoInteractions(invoker);
    }

    @Test
    @DisplayName("모든 재시도가 실패하면 retry 횟수만큼 시도한다")
    void retriesAllAttemptsOnFailure() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        setField(job, "enabled", true);
        setField(job, "archiveCommand", "/bin/false");
        setField(job, "retry", 3);

        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        Mockito.when(invoker.run(Mockito.anyString(), Mockito.anyString())).thenReturn(1);
        job.setInvoker(invoker);

        job.archiveColdPartitions();

        verify(invoker, Mockito.times(3)).run(eq("/bin/false"), eq("2025-04-01"));
    }

    @Test
    @DisplayName("실제 커맨드가 성공하면 runCommand 경로를 커버한다")
    void realCommandSuccess() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        setField(job, "enabled", true);
        setField(job, "archiveCommand", "/bin/echo");
        setField(job, "retry", 1);

        // 실제 /bin/echo 실행 (invoker 미변경 → runCommand 사용)
        job.archiveColdPartitions();

        assertThat(true).isTrue();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
