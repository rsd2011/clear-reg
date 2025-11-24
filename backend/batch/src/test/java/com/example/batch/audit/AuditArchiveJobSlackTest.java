package com.example.batch.audit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

class AuditArchiveJobSlackTest {

    @Test
    @DisplayName("재시도 후 실패하면 Slack 웹훅으로 알림을 보낸다")
    void notifyFailureOnRetriesExhausted() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        set(job, "enabled", true);
        set(job, "archiveCommand", "/bin/false");
        set(job, "retry", 2);
        set(job, "slackWebhook", "http://example.com/webhook");
        set(job, "alertEnabled", true);

        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        Mockito.when(invoker.run(anyString(), anyString())).thenReturn(1);
        job.setInvoker(invoker);

        RestTemplate rt = Mockito.mock(RestTemplate.class);
        job.setRestTemplate(rt);

        job.archiveColdPartitions();

        verify(rt).postForEntity(Mockito.eq("http://example.com/webhook"), Mockito.anyString(), Mockito.eq(String.class));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
