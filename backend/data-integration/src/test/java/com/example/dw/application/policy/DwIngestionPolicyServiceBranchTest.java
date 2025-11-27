package com.example.dw.application.policy;

import com.example.dw.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.example.dw.config.DwIngestionProperties;
import com.example.dw.config.DwIngestionProperties.JobScheduleProperties;
import com.example.dw.domain.HrIngestionPolicyEntity;
import com.example.dw.infrastructure.persistence.HrIngestionPolicyRepository;

class DwIngestionPolicyServiceBranchTest {

    HrIngestionPolicyRepository repo = mock(HrIngestionPolicyRepository.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    @Test
    @DisplayName("jobSchedules가 비어 있으면 fallback 스케줄로 유지된다")
    void mergeKeepsFallbackWhenEmpty() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwIngestionPolicyUpdateRequest req = new DwIngestionPolicyUpdateRequest(null, null, null, List.of());
        DwIngestionPolicyView view = service.update(req);

        assertThat(view.jobSchedules()).hasSize(1);
        assertThat(view.jobSchedules().getFirst().timezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("jobSchedules에 cron 없이 추가하면 예외를 던진다")
    void mergeCronMissingThrows() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwBatchJobScheduleRequest bad = new DwBatchJobScheduleRequest("JOB1", true, null, null);
        DwIngestionPolicyUpdateRequest req = new DwIngestionPolicyUpdateRequest(null, null, null, List.of(bad));

        assertThatThrownBy(() -> service.update(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("timezone만 주어지면 기존 cron을 유지하고 timezone을 덮어쓴다")
    void mergeTimezoneOnly() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwBatchJobScheduleRequest update = new DwBatchJobScheduleRequest("DW_INGESTION", null, "0 0 0 * * *", "Asia/Seoul");
        service.update(new DwIngestionPolicyUpdateRequest(null, null, null, List.of(update)));

        ArgumentCaptor<HrIngestionPolicyEntity> captor = ArgumentCaptor.forClass(HrIngestionPolicyEntity.class);
        verify(repo).save(captor.capture());
        String yaml = captor.getValue().getYaml();
        assertThat(yaml).contains("Asia/Seoul");
    }

    @Test
    @DisplayName("jobSchedules가 null이고 retention만 바꿔도 기존 스케줄을 유지한다")
    void mergeRetentionOnly() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwIngestionPolicyUpdateRequest req = new DwIngestionPolicyUpdateRequest(null, null, Duration.ofDays(60), null);
        DwIngestionPolicyView view = service.update(req);

        assertThat(view.retention()).isEqualTo(Duration.ofDays(60));
        assertThat(view.jobSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("enabled=false + cron 제공, timezone null이면 기존 timezone으로 채운다")
    void mergeEnabledFalseUsesExistingCronAndTimezone() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwBatchJobScheduleRequest update = new DwBatchJobScheduleRequest("DW_INGESTION", false, "0 0 0 * * *", null);
        DwIngestionPolicyView view = service.update(new DwIngestionPolicyUpdateRequest(null, null, null, List.of(update)));

        assertThat(view.jobSchedules().getFirst().enabled()).isFalse();
        assertThat(view.jobSchedules().getFirst().timezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("enabled null + cron/timezone 제공 시 enabled는 기존 값 유지")
    void mergeKeepsEnabledWhenNull() {
        DwIngestionProperties defaults = defaults("0 0 0 * * *", "UTC");
        when(repo.findByCode("dw.ingestion.policy")).thenReturn(Optional.empty());
        DwIngestionPolicyService service = new DwIngestionPolicyService(repo, defaults, publisher);

        DwBatchJobScheduleRequest update = new DwBatchJobScheduleRequest("DW_INGESTION", null, "0 0 12 * * *", "Asia/Seoul");
        DwIngestionPolicyView view = service.update(new DwIngestionPolicyUpdateRequest(null, null, null, List.of(update)));

        assertThat(view.jobSchedules().getFirst().enabled()).isTrue();
        assertThat(view.jobSchedules().getFirst().cronExpression()).isEqualTo("0 0 12 * * *");
        assertThat(view.jobSchedules().getFirst().timezone()).isEqualTo("Asia/Seoul");
    }

    private DwIngestionProperties defaults(String cron, String timezone) {
        DwIngestionProperties props = new DwIngestionProperties();
        props.setBatchCron(cron);
        props.setTimezone(timezone);
        JobScheduleProperties p = new JobScheduleProperties();
        p.setJobKey("DW_INGESTION");
        p.setEnabled(true);
        p.setCronExpression(cron);
        props.setJobSchedules(List.of(p));
        props.setRetention(Duration.ofDays(30));
        return props;
    }
}
