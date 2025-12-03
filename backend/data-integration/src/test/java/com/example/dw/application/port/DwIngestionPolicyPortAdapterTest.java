package com.example.dw.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.application.dto.DwBatchJobScheduleRequest;
import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwBatchJobScheduleView;
import com.example.dw.application.policy.DwIngestionPolicyService;
import com.example.dw.application.policy.DwIngestionPolicyView;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwIngestionPolicyPortAdapter 테스트")
class DwIngestionPolicyPortAdapterTest {

    @Mock
    private DwIngestionPolicyService policyService;

    @InjectMocks
    private DwIngestionPolicyPortAdapter adapter;

    @Test
    @DisplayName("Given 정책이 존재할 때 When 조회하면 Then 현재 정책을 반환한다")
    void givenPolicy_whenQuerying_thenReturnView() {
        DwIngestionPolicyView view = new DwIngestionPolicyView(
                "0 0 2 * * ?", "Asia/Seoul", Duration.ofDays(30),
                List.of(new DwBatchJobScheduleView("emp-sync", true, "0 0 3 * * ?", "Asia/Seoul")));
        given(policyService.view()).willReturn(view);

        DwIngestionPolicyView result = adapter.currentPolicy();

        assertThat(result.batchCron()).isEqualTo("0 0 2 * * ?");
        assertThat(result.timezone()).isEqualTo("Asia/Seoul");
        assertThat(result.jobSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("Given 유효한 요청 When 업데이트하면 Then 업데이트된 정책을 반환한다")
    void givenValidRequest_whenUpdating_thenReturnUpdatedView() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                "0 0 4 * * ?", "UTC", Duration.ofDays(60),
                List.of(new DwBatchJobScheduleRequest("emp-sync", false, "0 0 5 * * ?", "UTC")));
        DwIngestionPolicyView updatedView = new DwIngestionPolicyView(
                "0 0 4 * * ?", "UTC", Duration.ofDays(60),
                List.of(new DwBatchJobScheduleView("emp-sync", false, "0 0 5 * * ?", "UTC")));
        given(policyService.update(request)).willReturn(updatedView);

        DwIngestionPolicyView result = adapter.updatePolicy(request);

        assertThat(result.batchCron()).isEqualTo("0 0 4 * * ?");
        assertThat(result.retention()).isEqualTo(Duration.ofDays(60));
        verify(policyService).update(request);
    }
}
