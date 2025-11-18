package com.example.hr.application.policy;

import java.time.Duration;

public interface HrIngestionPolicyProvider {

    String batchCron();

    String timezone();

    Duration retention();
}
