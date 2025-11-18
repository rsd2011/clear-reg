package com.example.hr.application.policy;

import java.time.Duration;

public record HrIngestionPolicyUpdateRequest(String batchCron,
                                            String timezone,
                                            Duration retention) {
}
