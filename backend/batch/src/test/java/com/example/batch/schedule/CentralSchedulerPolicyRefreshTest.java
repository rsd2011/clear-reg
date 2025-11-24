package com.example.batch.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;

/**
 * Policy 변경 후 중앙 스케줄러가 바로 다음 트리거에 반영되는지 검증.
 */
class CentralSchedulerPolicyRefreshTest {

    @Test
    @DisplayName("Policy 스케줄 변경 시 nextExecution이 즉시 새 크론을 따른다")
    void appliesUpdatedPolicySchedule() {
        var provider = new InMemoryPolicyProvider(
                new BatchJobSchedule(true, com.example.common.schedule.TriggerType.CRON, "0 0 0 * * *", 0, 0, "UTC"));
        ScheduledJobPort port = new ScheduledJobPort() {
            @Override public String jobId() { return "policy-refresh"; }
            @Override public TriggerDescriptor trigger() {
                var s = provider.batchJobSchedule(BatchJobCode.FILE_SECURITY_RESCAN);
                return s.toTriggerDescriptor();
            }
            @Override public void runOnce(Instant now) { /* no-op */ }
        };

        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(java.util.List.of(port),
                java.time.Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC));

        scheduler.configureTasks(registrar);
        assertThat(registrar.trigger).isNotNull();

        var ctx = new SimpleTriggerContext();
        var next1 = registrar.trigger.nextExecution(ctx);

        // 정책 스케줄 변경
        provider.update(new BatchJobSchedule(true, com.example.common.schedule.TriggerType.CRON, "0 30 0 * * *", 0, 0, "UTC"));
        var next2 = registrar.trigger.nextExecution(new SimpleTriggerContext());

        assertThat(next2).isNotEqualTo(next1);
    }

    private static class RecordingRegistrar extends ScheduledTaskRegistrar {
        Trigger trigger;
        @Override
        public void addTriggerTask(Runnable task, Trigger trigger) {
            this.trigger = trigger;
        }
    }

    private static class InMemoryPolicyProvider implements PolicySettingsProvider {
        private final AtomicReference<BatchJobSchedule> ref;
        InMemoryPolicyProvider(BatchJobSchedule initial) { this.ref = new AtomicReference<>(initial); }
        void update(BatchJobSchedule schedule) { ref.set(schedule); }
        @Override public PolicyToggleSettings currentSettings() { return null; }
        @Override public BatchJobSchedule batchJobSchedule(BatchJobCode code) { return ref.get(); }
    }
}
