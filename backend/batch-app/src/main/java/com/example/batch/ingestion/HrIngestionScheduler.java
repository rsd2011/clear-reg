package com.example.batch.ingestion;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
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
                    CronExpression expression = CronExpression.parse(policyProvider.batchCron());
                    ZoneId zoneId = ZoneId.of(policyProvider.timezone());
                    ZonedDateTime last = getLastExecution(triggerContext, zoneId);
                    ZonedDateTime next = expression.next(last);
                    return next == null ? null : next.toInstant();
                });
    }

    private static ZonedDateTime getLastExecution(org.springframework.scheduling.TriggerContext triggerContext,
                                                  ZoneId zoneId) {
        Date lastCompletion = triggerContext.lastCompletionTime();
        if (lastCompletion != null) {
            return lastCompletion.toInstant().atZone(zoneId);
        }
        Date lastScheduled = triggerContext.lastScheduledExecutionTime();
        if (lastScheduled != null) {
            return lastScheduled.toInstant().atZone(zoneId);
        }
        return ZonedDateTime.now(zoneId);
    }
}
