package com.example.dw.application.policy;

public record DwBatchJobScheduleRequest(String jobKey,
                                        Boolean enabled,
                                        String cronExpression,
                                        String timezone) {
}
