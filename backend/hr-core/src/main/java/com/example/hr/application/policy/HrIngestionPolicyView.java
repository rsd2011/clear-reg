package com.example.hr.application.policy;

import java.time.Duration;

public record HrIngestionPolicyView(String batchCron, String timezone, Duration retention) {
}
