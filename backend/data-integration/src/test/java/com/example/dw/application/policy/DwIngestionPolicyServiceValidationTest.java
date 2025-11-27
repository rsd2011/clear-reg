package com.example.dw.application.policy;

import com.example.dw.application.dto.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

class DwIngestionPolicyServiceValidationTest {

    private DwIngestionPolicyService service;

    @BeforeEach
    void setUp() {
        HrIngestionPolicyRepository repository = mock(HrIngestionPolicyRepository.class);
        DwIngestionProperties defaults = new DwIngestionProperties();
        defaults.setBatchCron("0 0 * * * *");
        defaults.setTimezone("UTC");
        defaults.setRetention(Duration.ofDays(30));
        JobScheduleProperties schedule = new JobScheduleProperties();
        schedule.setJobKey("DW_INGESTION");
        schedule.setCronExpression("0 0 * * * *");
        defaults.setJobSchedules(List.of(schedule));
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        service = new DwIngestionPolicyService(repository, defaults, publisher);
    }

    @Test
    @DisplayName("jobKey가 없으면 업데이트를 거부한다")
    void rejectUpdateWhenJobKeyMissing() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest(null, true, "0 0 1 * * *", "UTC"))
        );

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobKey");
    }

    @Test
    @DisplayName("cronExpression이 없으면 업데이트를 거부한다")
    void rejectUpdateWhenCronMissing() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("NEW_JOB", true, null, "UTC"))
        );

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cronExpression");
    }

    @Test
    @DisplayName("timezone이 비어 있어도 cron이 존재하면 업데이트가 허용된다")
    void allowUpdateWhenTimezoneMissing() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("NEW_JOB", true, "0 0 * * * *", null))
        );

        service.update(request);
        // 예외가 없으면 성공
    }

    @Test
    @DisplayName("잘못된 cron 포맷이면 IllegalArgumentException을 던진다")
    void rejectUpdateWhenCronInvalid() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                null, null, null,
                List.of(new DwBatchJobScheduleRequest("JOB_BAD", true, "not-a-cron", "UTC"))
        );

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("잘못된 YAML로 초기화하려 하면 IllegalArgumentException을 던진다")
    void invalidYamlThrowsOnInit() {
        HrIngestionPolicyRepository repository = mock(HrIngestionPolicyRepository.class);
        var entity = new com.example.dw.domain.HrIngestionPolicyEntity("dw.ingestion.policy", "not: yaml: [");
        org.mockito.Mockito.when(repository.findByCode("dw.ingestion.policy")).thenReturn(java.util.Optional.of(entity));

        DwIngestionProperties defaults = new DwIngestionProperties();
        defaults.setBatchCron("0 0 * * * *");
        defaults.setTimezone("UTC");
        defaults.setRetention(Duration.ofDays(30));
        defaults.setJobSchedules(List.of());

        assertThatThrownBy(() -> new DwIngestionPolicyService(repository, defaults, mock(ApplicationEventPublisher.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
