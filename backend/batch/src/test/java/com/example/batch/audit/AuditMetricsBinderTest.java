package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditMetricsBinderTest {

    @Test
    @DisplayName("HOT/COLD/ObjectLock 지표가 게이지로 등록된다")
    void gaugesRegistered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuditMetricsBinder.AuditPartitionMetrics metrics = new AuditMetricsBinder.AuditPartitionMetrics();
        new AuditMetricsBinder(metrics).bindTo(registry);

        metrics.setHotIops(42);
        metrics.setColdCost(7);
        metrics.setObjectLockDelayMs(1234);

        assertThat(registry.get("audit_hot_iops_total").gauge().value()).isEqualTo(42.0);
        assertThat(registry.get("audit_cold_cost_estimate").gauge().value()).isEqualTo(7.0);
        assertThat(registry.get("audit_objectlock_delay_ms").gauge().value()).isEqualTo(1234.0);
    }
}
