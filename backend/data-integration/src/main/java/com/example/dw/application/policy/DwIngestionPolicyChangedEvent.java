package com.example.dw.application.policy;

import java.util.List;

public record DwIngestionPolicyChangedEvent(String timezone, List<DwBatchJobSchedule> jobSchedules) {
}
