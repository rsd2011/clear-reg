package com.example.batch.audit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

class AuditArchiveJobAlertTest {

    @Test
    @DisplayName("delayThreshold 초과 시 슬랙 경고를 보낸다")
    void sendsDelayAlert() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        set(job, "enabled", true);
        set(job, "archiveCommand", "/bin/echo");
        set(job, "retry", 1);
        set(job, "slackWebhook", "http://example.com/webhook");
        set(job, "alertEnabled", true);
        set(job, "delayThresholdMs", 0L); // 항상 경고

        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        Mockito.when(invoker.run(anyString(), anyString())).thenReturn(0);
        job.setInvoker(invoker);
        RestTemplate rt = Mockito.mock(RestTemplate.class);
        job.setRestTemplate(rt);

        job.archiveColdPartitions();

        verify(rt).postForEntity(Mockito.eq("http://example.com/webhook"), Mockito.anyString(), Mockito.eq(String.class));
    }

    @Test
    @DisplayName("alertEnabled=false면 알림을 보내지 않는다")
    void noAlertWhenDisabled() throws Exception {
        Clock clock = Clock.fixed(LocalDate.of(2025, 11, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditArchiveJob job = new AuditArchiveJob(clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        set(job, "enabled", true);
        set(job, "archiveCommand", "/bin/false");
        set(job, "retry", 1);
        set(job, "slackWebhook", "http://example.com/webhook");
        set(job, "alertEnabled", false);

        AuditArchiveJob.CommandInvoker invoker = Mockito.mock(AuditArchiveJob.CommandInvoker.class);
        Mockito.when(invoker.run(anyString(), anyString())).thenReturn(1);
        job.setInvoker(invoker);
        RestTemplate rt = Mockito.mock(RestTemplate.class);
        job.setRestTemplate(rt);

        job.archiveColdPartitions();

        verify(rt, never()).postForEntity(anyString(), anyString(), Mockito.eq(String.class));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
