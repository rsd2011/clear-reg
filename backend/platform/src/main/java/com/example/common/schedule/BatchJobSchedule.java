package com.example.common.schedule;

import java.time.ZoneId;
import java.util.Objects;

/**
 * 직렬화 친화적으로 zoneId를 String으로 보유하는 스케줄 정의.
 */
public record BatchJobSchedule(
        boolean enabled,
        TriggerType triggerType,
        String expression,           // cron expression when triggerType == CRON
        long fixedDelayMillis,       // when triggerType == FIXED_DELAY
        long initialDelayMillis,
        String zoneId                // optional, IANA id
) {
    public BatchJobSchedule {
        Objects.requireNonNull(triggerType, "triggerType");
        if (triggerType == TriggerType.CRON) {
            if (expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("cron expression required for CRON trigger");
            }
        } else if (triggerType == TriggerType.FIXED_DELAY) {
            if (fixedDelayMillis <= 0) {
                throw new IllegalArgumentException("fixedDelayMillis must be positive");
            }
        }
    }

    public TriggerDescriptor toTriggerDescriptor() {
        if (triggerType == TriggerType.CRON) {
            return new TriggerDescriptor(enabled, TriggerType.CRON, expression, 0, initialDelayMillis, resolveZone());
        }
        return new TriggerDescriptor(enabled, TriggerType.FIXED_DELAY, null, fixedDelayMillis, initialDelayMillis, resolveZone());
    }

    private ZoneId resolveZone() {
        if (zoneId == null || zoneId.isBlank()) {
            return ZoneId.systemDefault();
        }
        return ZoneId.of(zoneId);
    }
}
