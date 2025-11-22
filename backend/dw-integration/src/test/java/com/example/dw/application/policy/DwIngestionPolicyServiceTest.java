package com.example.dw.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.config.DwIngestionProperties;
import com.example.dw.domain.HrIngestionPolicyEntity;
import com.example.dw.infrastructure.persistence.HrIngestionPolicyRepository;
import com.example.testing.bdd.Scenario;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DwIngestionPolicyServiceTest {

    private static final String DOCUMENT_CODE = "dw.ingestion.policy";

    @Mock
    private HrIngestionPolicyRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DwIngestionProperties defaults;

    @BeforeEach
    void setUp() {
        defaults = new DwIngestionProperties();
        defaults.setBatchCron("0 0 1 * * *");
        defaults.setTimezone("UTC");
        defaults.setRetention(Duration.ofDays(45));
    }

    @Test
    void givenNoPersistedPolicy_whenView_thenReturnDefaults() {
        given(repository.findByCode(DOCUMENT_CODE)).willReturn(Optional.empty());

        Scenario.given("정책 서비스", () -> new DwIngestionPolicyService(repository, defaults, eventPublisher))
                .when("현재 상태 조회", DwIngestionPolicyService::view)
                .then("기본값 반환", view -> {
                    assertThat(view.batchCron()).isEqualTo(defaults.getBatchCron());
                    assertThat(view.timezone()).isEqualTo(defaults.getTimezone());
                    assertThat(view.retention()).isEqualTo(defaults.getRetention());
                    assertThat(view.jobSchedules()).hasSize(1);
                });
    }

    @Test
    void givenUpdateRequest_whenApply_thenPersistMergedState() {
        HrIngestionPolicyEntity persisted = new HrIngestionPolicyEntity(DOCUMENT_CODE, "batchCron: old");
        given(repository.findByCode(DOCUMENT_CODE)).willReturn(Optional.empty(), Optional.of(persisted));
        DwIngestionPolicyService service = new DwIngestionPolicyService(repository, defaults, eventPublisher);

        DwBatchJobScheduleRequest scheduleRequest = new DwBatchJobScheduleRequest("DW_INGESTION", true,
                "0 0 2 * * *", "Asia/Seoul");
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest("0 0 2 * * *", "Asia/Seoul",
                Duration.ofDays(60), List.of(scheduleRequest));

        Scenario.given("업데이트 요청", () -> service)
                .when("update 호출", svc -> svc.update(request))
                .then("새로운 뷰 반환", view -> {
                    assertThat(view.batchCron()).isEqualTo("0 0 2 * * *");
                    assertThat(view.retention()).isEqualTo(Duration.ofDays(60));
                })
                .and("저장 시 YAML 이 갱신", view -> {
                    ArgumentCaptor<HrIngestionPolicyEntity> captor = ArgumentCaptor.forClass(HrIngestionPolicyEntity.class);
                    verify(repository).save(captor.capture());
                    assertThat(captor.getValue().getYaml()).contains("0 0 2 * * *");
                    verify(eventPublisher).publishEvent(any(DwIngestionPolicyChangedEvent.class));
                });
    }

    @Test
    void givenInvalidYamlStored_whenInitializing_thenFailFast() {
        HrIngestionPolicyEntity invalid = new HrIngestionPolicyEntity(DOCUMENT_CODE, "invalid: : yaml");
        given(repository.findByCode(anyString())).willReturn(Optional.of(invalid));

        assertThatThrownBy(() -> new DwIngestionPolicyService(repository, defaults, eventPublisher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid DW ingestion policy");
    }

    @Test
    @DisplayName("YAML에 jobSchedules가 비어 있으면 기본 DW_INGESTION 스케줄로 대체한다")
    void givenEmptyJobSchedulesInYaml_whenInit_thenFallbackScheduleAdded() {
        String yaml = """
                batchCron: "0 0 6 * * *"
                timezone: "Asia/Seoul"
                retention: "PT72H"
                jobSchedules: []
                """;
        HrIngestionPolicyEntity entity = new HrIngestionPolicyEntity(DOCUMENT_CODE, yaml);
        given(repository.findByCode(DOCUMENT_CODE)).willReturn(Optional.of(entity));

        DwIngestionPolicyService service = new DwIngestionPolicyService(repository, defaults, eventPublisher);

        DwIngestionPolicyView view = service.view();

        assertThat(view.jobSchedules()).hasSize(1);
        DwBatchJobScheduleView schedule = view.jobSchedules().getFirst();
        assertThat(schedule.jobKey()).isEqualTo("DW_INGESTION");
        assertThat(schedule.cronExpression()).isEqualTo("0 0 6 * * *");
        assertThat(schedule.timezone()).isEqualTo("Asia/Seoul");
    }
}
