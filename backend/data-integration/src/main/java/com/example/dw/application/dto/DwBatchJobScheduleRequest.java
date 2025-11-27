package com.example.dw.application.dto;

public record DwBatchJobScheduleRequest(String jobKey,
                                        Boolean enabled,
                                        String cronExpression,
                                        String timezone) {
}
