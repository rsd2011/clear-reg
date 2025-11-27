package com.example.dwgateway.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dw.application.dto.DwBatchJobScheduleRequest;
import com.example.dw.application.policy.DwBatchJobScheduleView;
import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;
import com.example.dwgateway.dw.DwIngestionPolicyPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DwIngestionPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DwIngestionPolicyController 테스트")
class DwIngestionPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DwIngestionPolicyPort policyPort;

    @Test
    @DisplayName("Given 정책 When 조회하면 Then 현재 설정을 반환한다")
    void currentPolicy() throws Exception {
        DwIngestionPolicyView view = sampleView();
        given(policyPort.currentPolicy()).willReturn(view);

        mockMvc.perform(get("/api/admin/dw-ingestion/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCron").value("0 0 1 * * *"));
    }

    @Test
    @DisplayName("Given 업데이트 요청 When PUT 호출 Then 포트로 위임한다")
    void updatePolicy() throws Exception {
        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest("0 15 2 * * *", null,
                Duration.ofDays(60), List.of(new DwBatchJobScheduleRequest("DW_INGESTION", true,
                "0 15 2 * * *", null)));
        DwIngestionPolicyView view = sampleView();
        given(policyPort.updatePolicy(request)).willReturn(view);

        mockMvc.perform(put("/api/admin/dw-ingestion/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retention").value("PT720H"));

        then(policyPort).should().updatePolicy(request);
    }

    private DwIngestionPolicyView sampleView() {
        return new DwIngestionPolicyView("0 0 1 * * *", "Asia/Seoul", Duration.ofDays(30),
                List.of(new DwBatchJobScheduleView("DW_INGESTION", true, "0 0 1 * * *", "Asia/Seoul")));
    }
}
