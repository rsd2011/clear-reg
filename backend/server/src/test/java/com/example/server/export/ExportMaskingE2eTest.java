package com.example.server.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportMaskingE2eTest {

    @Autowired
    MockMvc mockMvc;

    @AfterEach
    void tearDown() {
        MaskingContextHolder.clear();
    }

    @Test
    @DisplayName("CSV export 시 기본 마스킹이 적용된다")
    void csvMaskedByDefault() throws Exception {
        MaskingContextHolder.set(MaskingTarget.builder().defaultMask(true).build());

        String body = mockMvc.perform(get("/api/exports/sample")
                        .param("accountNumber", "1234567890123456")
                        .param("reasonCode", "CS")
                        .param("legalBasisCode", "PIPA"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("************3456");
    }

    @Test
    @DisplayName("forceUnmask가 설정되면 원문이 그대로 내려온다")
    void csvForceUnmask() throws Exception {
        MaskingContextHolder.set(MaskingTarget.builder()
                .forceUnmask(true)
                .build());

        String body = mockMvc.perform(get("/api/exports/sample")
                        .param("accountNumber", "1234567890123456")
                        .param("reasonCode", "CS")
                        .param("legalBasisCode", "PIPA"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("1234567890123456");
    }
}
