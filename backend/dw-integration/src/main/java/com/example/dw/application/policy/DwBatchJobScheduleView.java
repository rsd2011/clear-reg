package com.example.dw.application.policy;

public record DwBatchJobScheduleView(String jobKey,
                                     boolean enabled,
                                     String cronExpression,
                                     String timezone) {
}
