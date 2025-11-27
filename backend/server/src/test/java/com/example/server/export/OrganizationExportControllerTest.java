package com.example.server.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.hamcrest.Matchers;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dw.application.export.writer.ExportWriterService;
import com.example.auth.security.JwtTokenProvider;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

@WebMvcTest(controllers = OrganizationExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class OrganizationExportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    HrOrganizationRepository repository;

    @MockBean
    ExportWriterService exportWriterService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("조직 Excel export 엔드포인트가 200을 반환한다")
    void exportOrgExcelReturnsOk() throws Exception {
        HrOrganizationEntity org = HrOrganizationEntity.snapshot("ORG1", 1, "본점", null, "ACTIVE",
                null, null, java.time.LocalDate.now(), null, java.util.UUID.randomUUID(), java.time.OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(org));
        given(exportWriterService.exportExcel(any(), anyList(), any(), anyString(), anyString(), any())).willReturn(new byte[0]);

        mockMvc.perform(get("/api/exports/orgs/excel")
                        .param("reasonCode", "RSN01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("organizations.xlsx")));
    }

    @Test
    @DisplayName("조직 PDF export 엔드포인트가 200을 반환한다")
    void exportOrgPdfReturnsOk() throws Exception {
        HrOrganizationEntity org = HrOrganizationEntity.snapshot("ORG1", 1, "본점", null, "ACTIVE",
                null, null, java.time.LocalDate.now(), null, java.util.UUID.randomUUID(), java.time.OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(org));
        given(exportWriterService.exportPdf(any(), anyList(), any(), anyString(), anyString(), any())).willReturn(new byte[0]);

        mockMvc.perform(get("/api/exports/orgs/pdf")
                        .param("reasonCode", "RSN01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("organizations.pdf")));
    }
}
