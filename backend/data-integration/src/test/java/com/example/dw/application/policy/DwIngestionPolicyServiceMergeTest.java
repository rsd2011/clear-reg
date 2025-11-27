package com.example.dw.application.policy;

import com.example.dw.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import com.example.dw.config.DwIngestionProperties;
import com.example.dw.config.DwIngestionProperties.JobScheduleProperties;
import com.example.dw.infrastructure.persistence.HrIngestionPolicyRepository;

class DwIngestionPolicyServiceMergeTest {

    private DwIngestionPolicyService service;

    @BeforeEach
    void setUp() {
        HrIngestionPolicyRepository repository = Mockito.mock(HrIngestionPolicyRepository.class);
        DwIngestionProperties defaults = new DwIngestionProperties();
        defaults.setBatchCron("0 0 * * * *");
        defaults.setTimezone("UTC");
        defaults.setRetention(Duration.ofDays(30));
        JobScheduleProperties schedule = new JobScheduleProperties();
        schedule.setJobKey("DW_INGESTION");
        schedule.setCronExpression("0 0 * * * *");
        defaults.setJobSchedules(List.of(schedule));
        service = new DwIngestionPolicyService(repository, defaults, Mockito.mock(ApplicationEventPublisher.class));
    }

    @Test
    @DisplayName("요청 스케줄이 비어 있으면 기존 스케줄을 유지한다")
    void keepsExistingSchedulesWhenRequestEmpty() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(null, null, null, List.of());

        DwIngestionPolicyView view = service.update(request);

        assertThat(view.jobSchedules()).hasSize(1);
        assertThat(view.jobSchedules().getFirst().jobKey()).isEqualTo("DW_INGESTION");
    }

    @Test
    @DisplayName("여러 신규 스케줄을 추가할 때 각기 다른 timezone을 보존하고 기본값은 마지막 요청으로 채운다")
    void mergesMultipleSchedulesPreservingTimezone() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, "Asia/Seoul", null,
                List.of(
                        new DwBatchJobScheduleRequest("JOB_A", true, "0 0 1 * * *", null),
                        new DwBatchJobScheduleRequest("JOB_B", true, "0 30 1 * * *", "America/LA")
                )
        );

        DwIngestionPolicyView view = service.update(request);

        assertThat(view.jobSchedules()).extracting(DwBatchJobScheduleView::jobKey)
                .contains("JOB_A", "JOB_B");
        assertThat(view.jobSchedules().stream()
                .filter(s -> s.jobKey().equals("JOB_A")).findFirst().orElseThrow().timezone())
                .isEqualTo("Asia/Seoul");
        assertThat(view.jobSchedules().stream()
                .filter(s -> s.jobKey().equals("JOB_B")).findFirst().orElseThrow().timezone())
                .isEqualTo("America/LA");
    }

    @Test
    @DisplayName("새로운 jobKey는 기존과 병합되며 timezone null은 기본값으로 채워진다")
    void mergesNewJobWithDefaultTimezone() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, "Asia/Seoul", null,
                List.of(new DwBatchJobScheduleRequest("NEW_JOB", true, "0 0 1 * * *", null))
        );

        DwIngestionPolicyView view = service.update(request);

        assertThat(view.jobSchedules()).extracting(DwBatchJobScheduleView::jobKey)
                .containsExactlyInAnyOrder("DW_INGESTION", "NEW_JOB");
        DwBatchJobScheduleView newJob = view.jobSchedules().stream()
                .filter(s -> s.jobKey().equals("NEW_JOB")).findFirst().orElseThrow();
        assertThat(newJob.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("중복 jobKey는 최신 요청 값으로 덮어쓴다")
    void overridesExistingJobWhenDuplicateKey() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("DW_INGESTION", false, "0 0 2 * * *", "UTC"))
        );

        DwIngestionPolicyView view = service.update(request);

        DwBatchJobScheduleView updated = view.jobSchedules().getFirst();
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.cronExpression()).isEqualTo("0 0 2 * * *");
    }

    @Test
    @DisplayName("요청과 기본 timezone 모두 null이면 기존 timezone으로 채운다")
    void fallsBackToExistingTimezoneWhenRequestNull() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("DW_INGESTION", true, "0 0 3 * * *", null))
        );

        DwIngestionPolicyView view = service.update(request);

        DwBatchJobScheduleView updated = view.jobSchedules().getFirst();
        assertThat(updated.timezone()).isEqualTo("UTC"); // 기존 기본값
    }

    @Test
    @DisplayName("중복된 jobKey 요청이 여러 개면 마지막 요청이 우선한다")
    void lastDuplicateRequestWins() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(
                        new DwBatchJobScheduleRequest("DW_INGESTION", true, "0 0 2 * * *", "UTC"),
                        new DwBatchJobScheduleRequest("DW_INGESTION", false, "0 15 2 * * *", "Asia/Seoul")
                )
        );

        DwIngestionPolicyView view = service.update(request);

        DwBatchJobScheduleView updated = view.jobSchedules().getFirst();
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.cronExpression()).isEqualTo("0 15 2 * * *");
        assertThat(updated.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("기존 스케줄 cron을 유지하고 enabled만 갱신할 수 있다")
    void keepsExistingCronWhenNotProvided() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("DW_INGESTION", false, null, null))
        );

        DwIngestionPolicyView view = service.update(request);

        DwBatchJobScheduleView updated = view.jobSchedules().getFirst();
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.cronExpression()).isEqualTo("0 0 * * * *"); // 기존 cron 유지
    }
}
