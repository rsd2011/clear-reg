package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;
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

class AuditArchiveJobSlackRetryTest {

    @Test
    @DisplayName("슬랙 알림은 실패 시 재시도 후 성공하면 더 이상 재시도하지 않는다")
    void slackAlertRetriesThenSucceeds() throws Exception {
        // given
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        // 첫 호출 실패, 두 번째 성공
        when(rest.postForEntity(any(String.class), any(), any(Class.class)))
                .thenThrow(new RuntimeException("network"))
                .thenReturn(ResponseEntity.ok("ok"));

        AuditArchiveJob job = new AuditArchiveJob(Clock.fixed(Instant.parse("2025-01-02T00:00:00Z"), ZoneOffset.UTC),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), () -> null);
        job.setRestTemplate(rest);
        job.setInvoker((cmd, target) -> 1); // 실패 exit code

        // 필수 프로퍼티 세팅
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "archiveCommand", "dummy");
        ReflectionTestUtils.setField(job, "retry", 2);
        ReflectionTestUtils.setField(job, "slackWebhook", "http://slack");
        ReflectionTestUtils.setField(job, "alertEnabled", true);
        ReflectionTestUtils.setField(job, "alertChannel", "#audit");

        // when
        job.archiveColdPartitions();

        // then
        verify(rest, times(2)).postForEntity(any(String.class), any(), any(Class.class));
    }
}
