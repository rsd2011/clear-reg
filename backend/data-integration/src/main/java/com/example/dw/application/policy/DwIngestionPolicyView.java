package com.example.dw.application.policy;

import java.time.Duration;
import java.util.List;

public record DwIngestionPolicyView(String batchCron,
                                    String timezone,
                                    Duration retention,
                                    List<DwBatchJobScheduleView> jobSchedules) {
}
