package com.example.batch.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

import com.example.dw.application.policy.DwBatchJobSchedule;
import com.example.dw.application.policy.DwIngestionPolicyProvider;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DwQuartzScheduleManagerTest {

    private static final String JOB_GROUP = "DW_BATCH";

    @Test
    @DisplayName("정책 스케줄이 활성화되면 기존 Quartz 잡을 지우고 새로 스케줄링한다")
    void refreshSchedules_replacesJobs_andStartsScheduler() throws Exception {
        // Given: 기존 잡이 존재하고 정책에서 DW_INGESTION 잡이 활성화되어 있을 때
        Scheduler scheduler = mock(Scheduler.class);
        DwIngestionPolicyProvider policyProvider = mock(DwIngestionPolicyProvider.class);
        DwBatchJobSchedule schedule = new DwBatchJobSchedule("DW_INGESTION", true, "0 0 1 * * ?", "UTC");
        when(policyProvider.jobSchedules()).thenReturn(List.of(schedule));
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP)))
                .thenReturn(Set.of(new JobKey("OLD", JOB_GROUP)));
        when(scheduler.isStarted()).thenReturn(false);

        DwQuartzScheduleManager manager = new DwQuartzScheduleManager(scheduler, policyProvider);

        // When: 초기화가 호출되면
        manager.initialize();

        // Then: 기존 잡을 삭제하고 새 잡을 스케줄하며 스케줄러를 시작한다
        verify(scheduler).deleteJobs(any(List.class));
        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        verify(scheduler).start();

        JobDetail jobDetail = jobDetailCaptor.getValue();
        assertThat(jobDetail.getKey()).isEqualTo(new JobKey("DW_INGESTION", JOB_GROUP));
        assertThat(triggerCaptor.getValue().getJobKey()).isEqualTo(jobDetail.getKey());
    }

    @Test
    @DisplayName("등록되지 않은 잡 키는 경고만 남기고 스케줄링하지 않는다")
    void unknownJobKey_isSkipped() throws Exception {
        // Given: 정책에 등록되지 않은 잡 키가 포함되어 있을 때
        Scheduler scheduler = mock(Scheduler.class);
        DwIngestionPolicyProvider policyProvider = mock(DwIngestionPolicyProvider.class);
        when(policyProvider.jobSchedules()).thenReturn(List.of(
                new DwBatchJobSchedule("UNKNOWN_JOB", true, "0 0 1 * * ?", "UTC")
        ));
        when(scheduler.getJobKeys(any())).thenReturn(Set.of());
        when(scheduler.isStarted()).thenReturn(false);

        DwQuartzScheduleManager manager = new DwQuartzScheduleManager(scheduler, policyProvider);

        // When
        manager.initialize();

        // Then: scheduleJob은 호출되지 않고 스케줄러는 시작된다
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler).start();
    }

    @Test
    @DisplayName("스케줄 목록이 없으면 잡을 삭제만 하고 스케줄링하지 않는다")
    void skipSchedulingWhenNoSchedules() throws Exception {
        // Given
        Scheduler scheduler = mock(Scheduler.class);
        DwIngestionPolicyProvider policyProvider = mock(DwIngestionPolicyProvider.class);
        when(policyProvider.jobSchedules()).thenReturn(null);
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP))).thenReturn(Set.of());

        DwQuartzScheduleManager manager = new DwQuartzScheduleManager(scheduler, policyProvider);

        // When
        manager.initialize();

        // Then
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).start();
    }

    @Test
    @DisplayName("비활성화된 스케줄은 건너뛰고 스케줄러는 시작하지 않는다")
    void skipDisabledSchedules() throws Exception {
        // Given
        Scheduler scheduler = mock(Scheduler.class);
        DwIngestionPolicyProvider policyProvider = mock(DwIngestionPolicyProvider.class);
        DwBatchJobSchedule disabled = new DwBatchJobSchedule("DW_INGESTION", false, "0 0 1 * * ?", "UTC");
        when(policyProvider.jobSchedules()).thenReturn(List.of(disabled));
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP))).thenReturn(Set.of());
        when(scheduler.isStarted()).thenReturn(true); // 이미 실행 중

        DwQuartzScheduleManager manager = new DwQuartzScheduleManager(scheduler, policyProvider);

        // When
        manager.initialize();

        // Then
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).start(); // already started
    }

    @Test
    @DisplayName("스케줄러가 이미 시작된 경우에는 추가로 start를 호출하지 않는다")
    void doesNotStartIfAlreadyStarted() throws Exception {
        // Given
        Scheduler scheduler = mock(Scheduler.class);
        DwIngestionPolicyProvider policyProvider = mock(DwIngestionPolicyProvider.class);
        DwBatchJobSchedule schedule = new DwBatchJobSchedule("DW_INGESTION", true, "0 0 1 * * ?", "UTC");
        when(policyProvider.jobSchedules()).thenReturn(List.of(schedule));
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP))).thenReturn(Set.of());
        when(scheduler.isStarted()).thenReturn(true);

        DwQuartzScheduleManager manager = new DwQuartzScheduleManager(scheduler, policyProvider);

        // When
        manager.initialize();

        // Then
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).start();
    }
}
