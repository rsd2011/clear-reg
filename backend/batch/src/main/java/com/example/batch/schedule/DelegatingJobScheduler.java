package com.example.batch.schedule;

import java.time.Clock;
import java.util.List;

import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DelegatingJobScheduler implements SchedulingConfigurer {

    private final List<ScheduledJobPort> jobPorts;
    private final Clock clock;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (jobPorts == null || jobPorts.isEmpty()) {
            log.debug("[central-scheduler] no delegated jobs registered");
            return;
        }
        jobPorts.forEach(port -> {
            TriggerDescriptor initial = port.trigger();
            if (initial == null || !initial.enabled()) {
                log.info("[central-scheduler] {} disabled; skipping registration", port.jobId());
                return;
            }
            taskRegistrar.addTriggerTask(() -> port.runOnce(clock.instant()), triggerContext -> {
                TriggerDescriptor descriptor = port.trigger();
                if (descriptor == null || !descriptor.enabled()) {
                    log.info("[central-scheduler] {} disabled at runtime; skipping execution", port.jobId());
                    return null;
                }
                return descriptor.toTrigger().nextExecution(triggerContext);
            });
            log.info("[central-scheduler] registered job={} (dynamic trigger)", port.jobId());
        });
    }
}
