package com.example.dw.application.policy;

import java.time.Duration;
import java.util.List;

public record DwIngestionPolicyUpdateRequest(String batchCron,
                                            String timezone,
                                            Duration retention,
                                            List<DwBatchJobScheduleRequest> jobSchedules) {
}
