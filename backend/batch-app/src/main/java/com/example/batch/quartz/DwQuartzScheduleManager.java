package com.example.batch.quartz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.dw.application.policy.DwBatchJobSchedule;
import com.example.dw.application.policy.DwIngestionPolicyChangedEvent;
import com.example.dw.application.policy.DwIngestionPolicyProvider;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DwQuartzScheduleManager {

    private static final String JOB_GROUP = "DW_BATCH";

    private final Scheduler scheduler;
    private final DwIngestionPolicyProvider policyProvider;
    private final Map<String, Class<? extends Job>> jobRegistry = Map.of("DW_INGESTION", DwIngestionQuartzJob.class);

    public DwQuartzScheduleManager(Scheduler scheduler, DwIngestionPolicyProvider policyProvider) {
        this.scheduler = scheduler;
        this.policyProvider = policyProvider;
    }

    @PostConstruct
    public void initialize() {
        refreshSchedules(policyProvider.jobSchedules());
    }

    @EventListener
    public void onPolicyChanged(DwIngestionPolicyChangedEvent event) {
        refreshSchedules(event.jobSchedules());
    }

    private void refreshSchedules(List<DwBatchJobSchedule> schedules) {
        try {
            clearExistingJobs();
            if (schedules == null) {
                return;
            }
            for (DwBatchJobSchedule schedule : schedules) {
                if (!schedule.enabled()) {
                    continue;
                }
                scheduleJob(schedule);
            }
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } catch (SchedulerException exception) {
            log.error("Failed to refresh Quartz schedules", exception);
        }
    }

    private void clearExistingJobs() throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP));
        if (!jobKeys.isEmpty()) {
            scheduler.deleteJobs(new ArrayList<>(jobKeys));
        }
    }

    private void scheduleJob(DwBatchJobSchedule schedule) throws SchedulerException {
        Class<? extends Job> jobClass = jobRegistry.get(schedule.jobKey());
        if (jobClass == null) {
            log.warn("Unknown job key '{}', skipping scheduling", schedule.jobKey());
            return;
        }
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(schedule.jobKey(), JOB_GROUP)
                .storeDurably(false)
                .build();

        CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(schedule.cronExpression())
                .inTimeZone(java.util.TimeZone.getTimeZone(schedule.timezone()));
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(schedule.jobKey() + "_trigger", JOB_GROUP)
                .withSchedule(cronBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled DW job {} with cron {} ({})", schedule.jobKey(), schedule.cronExpression(),
                schedule.timezone());
    }
}
