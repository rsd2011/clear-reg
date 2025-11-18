package com.example.batch.ingestion;

import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.hr.application.policy.HrIngestionPolicyProvider;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hr.ingestion", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class HrIngestionScheduler implements SchedulingConfigurer {

    private final HrIngestionService ingestionService;
    private final HrIngestionPolicyProvider policyProvider;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(() ->
                        ingestionService.ingestNextFile().ifPresent(batch ->
                                log.info("Completed scheduled HR batch {} with status {}", batch.getId(), batch.getStatus())),
                triggerContext -> {
                    String cron = policyProvider.batchCron();
                    ZoneId zoneId = ZoneId.of(policyProvider.timezone());
                    CronTrigger trigger = new CronTrigger(cron, zoneId);
                    var next = trigger.nextExecutionTime(triggerContext);
                    return next == null ? null : next.toInstant();
                });
    }
}
