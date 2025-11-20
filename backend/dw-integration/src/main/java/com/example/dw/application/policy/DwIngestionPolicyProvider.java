package com.example.dw.application.policy;

import java.time.Duration;
import java.util.List;

public interface DwIngestionPolicyProvider {

    String batchCron();

    String timezone();

    Duration retention();

    List<DwBatchJobSchedule> jobSchedules();
}
