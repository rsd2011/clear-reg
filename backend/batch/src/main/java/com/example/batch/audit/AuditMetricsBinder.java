package com.example.batch.audit;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * HOT/COLD/Object Lock 관련 메트릭을 노출하기 위한 MeterBinder.
 * 실제 IOPS/비용/지연 값은 운영 수집기로부터 주입해 갱신한다.
 */
@Component
@RequiredArgsConstructor
public class AuditMetricsBinder implements MeterBinder {

    private final AuditPartitionMetrics metrics;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("audit_hot_iops_total", metrics::getHotIops).register(registry);
        Gauge.builder("audit_cold_cost_estimate", metrics::getColdCost).register(registry);
        Gauge.builder("audit_objectlock_delay_ms", metrics::getObjectLockDelayMs).register(registry);
    }

    /**
     * 값은 외부 수집기/스케줄러에서 주입해 업데이트한다.
     */
    @Component
    public static class AuditPartitionMetrics {
        private final AtomicLong hotIops = new AtomicLong();
        private final AtomicLong coldCost = new AtomicLong();
        private final AtomicLong objectLockDelayMs = new AtomicLong();

        public long getHotIops() {
            return hotIops.get();
        }

        public long getColdCost() {
            return coldCost.get();
        }

        public long getObjectLockDelayMs() {
            return objectLockDelayMs.get();
        }

        public void setHotIops(long value) {
            hotIops.set(value);
        }

        public void setColdCost(long value) {
            coldCost.set(value);
        }

        public void setObjectLockDelayMs(long value) {
            objectLockDelayMs.set(value);
        }
    }
}
