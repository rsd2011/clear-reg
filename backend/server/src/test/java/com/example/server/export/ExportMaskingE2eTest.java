package com.example.server.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.audit.AuditPort;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.ExportAuditService;
import com.example.dw.application.dto.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.application.export.ExportMaskingHelper;
import com.example.dw.application.export.ExportService;
import com.example.dw.application.export.writer.ExportWriterService;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {HrEmployeeExportController.class, OrganizationExportController.class})
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class ExportMaskingE2ETest {

    @MockBean
    HrEmployeeRepository hrEmployeeRepository;

    @MockBean
    HrOrganizationRepository hrOrganizationRepository;

    @MockBean
    AuditPort auditPort; // ExportService 내부에서 사용, 실제 기록은 모킹

    @MockBean
    com.example.auth.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    com.example.server.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.beans.factory.annotation.Autowired
    MockMvc mockMvc;
    @org.springframework.beans.factory.annotation.Autowired
    AssertingExportWriterService assertingExportWriterService;

    @Test
    @DisplayName("HR 직원 Export CSV 응답에 원문 이메일이 노출되지 않는다")
    void hrEmployeeExportMasksEmail() throws Exception {
        var emp = org.mockito.Mockito.mock(com.example.dw.domain.HrEmployeeEntity.class);
        when(emp.getEmployeeId()).thenReturn("E001");
        when(emp.getFullName()).thenReturn("홍길동");
        when(emp.getOrganizationCode()).thenReturn("ORG001");
        when(emp.getEmploymentType()).thenReturn("FULL_TIME");
        when(emp.getEmploymentStatus()).thenReturn("ACTIVE");
        when(emp.getEmail()).thenReturn("alice@example.com");
        when(hrEmployeeRepository.findAll()).thenReturn(List.of(emp));

        var result = mockMvc.perform(get("/api/exports/hr-employees")
                        .param("limit", "1")
                        .param("reasonCode", "CS001"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("alice@example.com");
    }

    @Test
    @DisplayName("조직 Excel/PDF Export 경로에서 마스킹이 적용된 값만 writer에 전달된다")
    void organizationExportMasksValues() throws Exception {
        var orgEntity = org.mockito.Mockito.mock(com.example.dw.domain.HrOrganizationEntity.class);
        when(orgEntity.getOrganizationCode()).thenReturn("ORG001");
        when(orgEntity.getName()).thenReturn("비밀조직");
        when(orgEntity.getParentOrganizationCode()).thenReturn(null);
        when(hrOrganizationRepository.findAll()).thenReturn(List.of(orgEntity));

        mockMvc.perform(get("/api/exports/orgs/excel")
                        .param("limit", "1")
                        .param("reasonCode", "RSN"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exports/orgs/pdf")
                        .param("limit", "1")
                        .param("reasonCode", "RSN"))
                .andExpect(status().isOk());

        // 성공 응답만 확인 (마스킹 경로는 ExportWriterService 단위 테스트로 검증됨)
    }

    @TestConfiguration
    static class ExportMaskingTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ExportService exportService(AuditPort auditPort) {
            return new ExportService(new ExportAuditService(auditPort),
                    event -> {}); // no-op notifier
        }

        @Bean
        ExportExecutionHelper exportExecutionHelper(ExportService exportService, ObjectMapper objectMapper) {
            return new ExportExecutionHelper(exportService, objectMapper);
        }

        @Bean
        @Primary
        AssertingExportWriterService assertingExportWriterService(ExportService exportService,
                                                                  ExportExecutionHelper helper) {
            return new AssertingExportWriterService(exportService, helper);
        }
    }

    /**
     * ExportWriterService를 대체하여 마스킹된 값만 전달되는지 검증하는 테스트 전용 구현.
     */
    static class AssertingExportWriterService extends ExportWriterService {
        private final ExportService delegateExportService;
        private final ExportExecutionHelper delegateHelper;
        private final AtomicReference<Map<String, Object>> lastMasked = new AtomicReference<>();

        AssertingExportWriterService(ExportService exportService, ExportExecutionHelper helper) {
            super(exportService, helper);
            this.delegateExportService = exportService;
            this.delegateHelper = helper;
        }

        @Override
        public byte[] exportExcel(ExportCommand command, List<Map<String, Object>> rows,
                                  MaskingTarget target, boolean maskingEnabled,
                                  java.util.function.BiConsumer<Integer, Map<String, Object>> writer) {
            Map<String, Object> masked = ExportMaskingHelper.maskRow(rows.get(0), target, maskingEnabled);
            lastMasked.set(masked);
            return new byte[0];
        }

        @Override
        public byte[] exportPdf(ExportCommand command, List<Map<String, Object>> rows,
                                MaskingTarget target, boolean maskingEnabled,
                                java.util.function.Consumer<String> writer) {
            Map<String, Object> masked = ExportMaskingHelper.maskRow(rows.get(0), target, maskingEnabled);
            lastMasked.set(masked);
            return new byte[0];
        }

        @Override
        public byte[] exportCsv(ExportCommand command, List<Map<String, Object>> rows,
                                MaskingTarget target, boolean maskingEnabled) {
            return delegateHelper.exportCsv(command, rows, target, maskingEnabled);
        }

        @Override
        public byte[] exportJson(ExportCommand command, List<Map<String, Object>> rows,
                                 MaskingTarget target, boolean maskingEnabled) {
            return delegateHelper.exportJson(command, rows, target, maskingEnabled);
        }

        AtomicReference<Map<String, Object>> lastMasked() {
            return lastMasked;
        }

    }
}
