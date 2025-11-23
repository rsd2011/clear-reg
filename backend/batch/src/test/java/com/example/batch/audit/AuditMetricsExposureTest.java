package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=prometheus",
        "management.endpoint.prometheus.enabled=true",
        "audit.archive.enabled=false" // 실행은 막지만 빈 초기화로 메트릭은 등록됨
})
class AuditMetricsExposureTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("Prometheus 엔드포인트에 audit 전용 메트릭이 노출된다")
    void prometheusContainsAuditMetrics() {
        String body = rest.getForObject("/actuator/prometheus", String.class);
        assertThat(body).contains("audit_archive_success_total");
        assertThat(body).contains("audit_archive_failure_total");
        assertThat(body).contains("audit_archive_elapsed_ms");
    }
}
