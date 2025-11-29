package com.example.server.export;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.auth.security.JwtTokenProvider;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.domain.HrEmployeeEntity;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@WebMvcTest(controllers = HrEmployeeExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class HrEmployeeExportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    HrEmployeeRepository repository;

    @MockBean
    ExportExecutionHelper helper;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("HR 직원 CSV export 엔드포인트는 200을 반환한다")
    void exportHrEmployeesReturnsOk() throws Exception {
        HrEmployeeEntity emp = HrEmployeeEntity.snapshot("E001", 1, "Alice", "alice@example.com",
                "ORG1", "FULL", "ACTIVE", java.time.LocalDate.now(), null, UUID.randomUUID(), java.time.OffsetDateTime.now());

        given(repository.findAll()).willReturn(List.of(emp));
        given(helper.exportCsv(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any(), anyBoolean())).willReturn("id\nE001".getBytes());

        MaskingContextHolder.set(MaskingTarget.builder().defaultMask(true).build());

        mockMvc.perform(get("/api/exports/hr-employees")
                        .param("reasonCode", "RSN01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("hr_employees.csv")));

        MaskingContextHolder.clear();
    }
}
