package com.example.batch.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;
import com.example.common.schedule.TriggerType;

class DelegatingJobSchedulerTest {

    @Test
    @DisplayName("등록된 포트를 TriggerTask 로 위임 등록한다")
    void registersDelegatedJobs() {
        AtomicInteger runs = new AtomicInteger();
        ScheduledJobPort port = new ScheduledJobPort() {
            @Override public String jobId() { return "test-job"; }
            @Override public TriggerDescriptor trigger() {
                return new TriggerDescriptor(true, TriggerType.FIXED_DELAY, null, 1000, 0, null);
            }
            @Override public void runOnce(Instant now) { runs.incrementAndGet(); }
        };
        ScheduledJobPort cronPort = new ScheduledJobPort() {
            @Override public String jobId() { return "cron-job"; }
            @Override public TriggerDescriptor trigger() {
                return new TriggerDescriptor(true, TriggerType.CRON, "0 0 * * * *", 0, 0, null);
            }
            @Override public void runOnce(Instant now) { runs.incrementAndGet(); }
        };

        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(port, cronPort),
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC));

        scheduler.configureTasks(registrar);

        assertThat(registrar.tasks).hasSize(2);
        // 실행도 가능해야 한다
        registrar.tasks.getFirst().run();
        assertThat(runs).hasValue(1);
    }

    @Test
    @DisplayName("빈 포트 목록은 등록을 건너뛴다")
    void skipsWhenNoPorts() {
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(),
                Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.tasks).isEmpty();
    }

    @Test
    @DisplayName("포트 목록이 null이면 등록을 건너뛴다")
    void skipsWhenPortsNull() {
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(null, Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.tasks).isEmpty();
    }

    @Test
    @DisplayName("비활성화된 포트는 등록하지 않는다")
    void skipsDisabledPort() {
        ScheduledJobPort port = new ScheduledJobPort() {
            @Override public String jobId() { return "disabled"; }
            @Override public TriggerDescriptor trigger() { return new TriggerDescriptor(false, TriggerType.CRON, "0 0 * * * *", 0, 0, null); }
            @Override public void runOnce(Instant now) { }
        };
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(port), Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.tasks).isEmpty();
    }

    @Test
    @DisplayName("trigger가 null이면 등록하지 않는다")
    void skipsNullTrigger() {
        ScheduledJobPort port = new ScheduledJobPort() {
            @Override public String jobId() { return "null-trigger"; }
            @Override public TriggerDescriptor trigger() { return null; }
            @Override public void runOnce(Instant now) { }
        };
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(port), Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.tasks).isEmpty();
    }

    private static class RecordingRegistrar extends ScheduledTaskRegistrar {
        final List<Runnable> tasks = new ArrayList<>();
        final List<Trigger> triggers = new ArrayList<>();

        @Override
        public void addTriggerTask(Runnable task, Trigger trigger) {
            tasks.add(task);
            triggers.add(trigger);
        }
    }

    @Test
    @DisplayName("동적으로 비활성화된 포트는 다음 실행을 건너뛴다")
    void skipsWhenPortBecomesDisabled() {
        AtomicInteger calls = new AtomicInteger();
        ScheduledJobPort port = new ScheduledJobPort() {
            boolean enabled = true;
            @Override public String jobId() { return "toggle-job"; }
            @Override public TriggerDescriptor trigger() {
                boolean e = enabled;
                enabled = false; // 다음 호출부터 disable
                return new TriggerDescriptor(e, TriggerType.FIXED_DELAY, null, 1000, 0, null);
            }
            @Override public void runOnce(Instant now) { calls.incrementAndGet(); }
        };
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(port), Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.triggers).hasSize(1);
        Trigger trigger = registrar.triggers.getFirst();
        // 런타임에 disable 되면 nextExecution이 null을 반환한다
        assertThat(trigger.nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext())).isNull();
    }

    @Test
    @DisplayName("런타임에 trigger가 null이 되면 실행을 건너뛴다")
    void skipsWhenTriggerBecomesNullAtRuntime() {
        AtomicInteger triggerCalls = new AtomicInteger();
        ScheduledJobPort port = new ScheduledJobPort() {
            @Override public String jobId() { return "null-runtime-trigger"; }
            @Override public TriggerDescriptor trigger() {
                // 처음에는 enabled, 두 번째 호출부터 null 반환
                if (triggerCalls.getAndIncrement() == 0) {
                    return new TriggerDescriptor(true, TriggerType.FIXED_DELAY, null, 1000, 0, null);
                }
                return null;
            }
            @Override public void runOnce(Instant now) { }
        };
        RecordingRegistrar registrar = new RecordingRegistrar();
        DelegatingJobScheduler scheduler = new DelegatingJobScheduler(List.of(port), Clock.systemUTC());

        scheduler.configureTasks(registrar);

        assertThat(registrar.triggers).hasSize(1);
        Trigger trigger = registrar.triggers.getFirst();
        // 런타임에 trigger가 null이 되면 nextExecution도 null
        assertThat(trigger.nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext())).isNull();
    }
}
