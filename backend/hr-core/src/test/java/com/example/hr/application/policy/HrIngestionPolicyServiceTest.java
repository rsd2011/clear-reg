package com.example.hr.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hr.config.HrIngestionProperties;
import com.example.hr.domain.HrIngestionPolicyEntity;
import com.example.hr.infrastructure.persistence.HrIngestionPolicyRepository;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class HrIngestionPolicyServiceTest {

    private static final String DOCUMENT_CODE = "hr.ingestion.policy";

    @Mock
    private HrIngestionPolicyRepository repository;

    private HrIngestionProperties defaults;

    @BeforeEach
    void setUp() {
        defaults = new HrIngestionProperties();
        defaults.setBatchCron("0 0 1 * * *");
        defaults.setTimezone("UTC");
        defaults.setRetention(Duration.ofDays(45));
    }

    @Test
    void givenNoPersistedPolicy_whenView_thenReturnDefaults() {
        given(repository.findByCode(DOCUMENT_CODE)).willReturn(Optional.empty());

        Scenario.given("정책 서비스", () -> new HrIngestionPolicyService(repository, defaults))
                .when("현재 상태 조회", HrIngestionPolicyService::view)
                .then("기본값 반환", view -> {
                    assertThat(view.batchCron()).isEqualTo(defaults.getBatchCron());
                    assertThat(view.timezone()).isEqualTo(defaults.getTimezone());
                    assertThat(view.retention()).isEqualTo(defaults.getRetention());
                });
    }

    @Test
    void givenUpdateRequest_whenApply_thenPersistMergedState() {
        HrIngestionPolicyEntity persisted = new HrIngestionPolicyEntity(DOCUMENT_CODE, "batchCron: old");
        given(repository.findByCode(DOCUMENT_CODE)).willReturn(Optional.empty(), Optional.of(persisted));
        HrIngestionPolicyService service = new HrIngestionPolicyService(repository, defaults);

        HrIngestionPolicyUpdateRequest request = new HrIngestionPolicyUpdateRequest("0 0 2 * * *", "Asia/Seoul",
                Duration.ofDays(60));

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
                });
    }

    @Test
    void givenInvalidYamlStored_whenInitializing_thenFailFast() {
        HrIngestionPolicyEntity invalid = new HrIngestionPolicyEntity(DOCUMENT_CODE, "invalid: : yaml");
        given(repository.findByCode(anyString())).willReturn(Optional.of(invalid));

        assertThatThrownBy(() -> new HrIngestionPolicyService(repository, defaults))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid HR ingestion policy YAML");
    }
}
