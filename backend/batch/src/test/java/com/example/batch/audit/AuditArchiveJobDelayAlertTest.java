package com.example.batch.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
class AuditArchiveJobDelayAlertTest {

    @Test
    @DisplayName("실행 시간이 임계값을 넘으면 지연 알림을 재시도 포함해 전송한다")
    void delayAlertWithRetry() throws Exception {
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        // 첫 알림 실패, 두 번째 성공
        when(rest.postForEntity(any(String.class), any(), any(Class.class)))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(ResponseEntity.ok("ok"));

        AuditArchiveJob job = new AuditArchiveJob(Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        job.setRestTemplate(rest);
        // invoker는 성공하지만 느린 케이스로 가정
        job.setInvoker((cmd, target) -> {
            Thread.sleep(50);
            return 0;
        });

        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "archiveCommand", "dummy");
        ReflectionTestUtils.setField(job, "retry", 2);
        ReflectionTestUtils.setField(job, "slackWebhook", "http://slack");
        ReflectionTestUtils.setField(job, "alertEnabled", true);
        ReflectionTestUtils.setField(job, "alertChannel", "#audit");
        ReflectionTestUtils.setField(job, "delayThresholdMs", 10L);

        job.archiveColdPartitions();

        // 재시도 2회(실패 후 성공) 호출 확인
        verify(rest, times(2)).postForEntity(any(String.class), any(), any(Class.class));
    }
}
