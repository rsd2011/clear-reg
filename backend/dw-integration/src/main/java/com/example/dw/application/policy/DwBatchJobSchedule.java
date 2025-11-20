package com.example.dw.application.policy;

import java.util.Objects;

public record DwBatchJobSchedule(String jobKey,
                                 boolean enabled,
                                 String cronExpression,
                                 String timezone) {

    public DwBatchJobSchedule {
        Objects.requireNonNull(jobKey, "jobKey must not be null");
        Objects.requireNonNull(cronExpression, "cronExpression must not be null");
    }

    public DwBatchJobSchedule withDefaults(String fallbackTimezone) {
        return new DwBatchJobSchedule(jobKey, enabled, cronExpression,
                timezone != null ? timezone : fallbackTimezone);
    }
}
