package com.example.common.schedule;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

public record TriggerDescriptor(
        boolean enabled,
        TriggerType type,
        @Nullable String expression,
        long fixedDelayMillis,
        long initialDelayMillis,
        @Nullable ZoneId zoneId
) {

    public TriggerDescriptor {
        Objects.requireNonNull(type, "type");
        if (type == TriggerType.CRON && (expression == null || expression.isBlank())) {
            throw new IllegalArgumentException("cron expression required for CRON trigger");
        }
        if (type == TriggerType.FIXED_DELAY && fixedDelayMillis <= 0) {
            throw new IllegalArgumentException("fixedDelayMillis must be positive for FIXED_DELAY trigger");
        }
    }

    public static TriggerDescriptor cron(String expression) {
        return new TriggerDescriptor(true, TriggerType.CRON, expression, 0, 0, ZoneId.systemDefault());
    }

    public static TriggerDescriptor cron(String expression, ZoneId zoneId) {
        return new TriggerDescriptor(true, TriggerType.CRON, expression, 0, 0, zoneId);
    }

    public static TriggerDescriptor fixedDelay(long delayMillis) {
        return new TriggerDescriptor(true, TriggerType.FIXED_DELAY, null, delayMillis, 0, null);
    }

    public Trigger toTrigger() {
        if (!enabled) {
            return triggerContext -> null;
        }
        return switch (type) {
            case CRON -> new CronTrigger(expression, zoneId == null ? ZoneId.systemDefault() : zoneId);
            case FIXED_DELAY -> {
                PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(fixedDelayMillis));
                trigger.setInitialDelay(Duration.ofMillis(initialDelayMillis));
                yield trigger;
            }
        };
    }
}
