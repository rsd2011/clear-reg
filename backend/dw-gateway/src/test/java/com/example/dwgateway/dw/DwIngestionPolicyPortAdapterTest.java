package com.example.dwgateway.dw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.application.policy.DwBatchJobScheduleRequest;
import com.example.dw.application.policy.DwBatchJobScheduleView;
import com.example.dw.application.policy.DwIngestionPolicyService;
import com.example.dw.application.policy.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;

@DisplayName("DwIngestionPolicyPortAdapter 테스트")
class DwIngestionPolicyPortAdapterTest {

    private final DwIngestionPolicyService policyService = Mockito.mock(DwIngestionPolicyService.class);
    private final DwIngestionPolicyPortAdapter adapter = new DwIngestionPolicyPortAdapter(policyService);

    @Test
    @DisplayName("현재 정책 조회를 위임한다")
    void currentPolicy() {
        DwIngestionPolicyView view = sampleView();
        given(policyService.view()).willReturn(view);

        assertThat(adapter.currentPolicy()).isEqualTo(view);
        then(policyService).should().view();
    }

    @Test
    @DisplayName("업데이트를 위임한다")
    void updatePolicy() {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest("0 0 1 * * *", "Asia/Seoul",
                Duration.ofDays(30), List.of(new DwBatchJobScheduleRequest("DW_INGESTION", true, "0 0 1 * * *", null)));
        DwIngestionPolicyView view = sampleView();
        given(policyService.update(request)).willReturn(view);

        assertThat(adapter.updatePolicy(request)).isEqualTo(view);
        then(policyService).should().update(request);
    }

    private DwIngestionPolicyView sampleView() {
        return new DwIngestionPolicyView("0 0 1 * * *", "Asia/Seoul", Duration.ofDays(30),
                List.of(new DwBatchJobScheduleView("DW_INGESTION", true, "0 0 1 * * *", "Asia/Seoul")));
    }
}
