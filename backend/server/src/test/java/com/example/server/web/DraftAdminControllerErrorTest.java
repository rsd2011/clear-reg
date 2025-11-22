package com.example.server.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.permission.RequirePermissionAspect;
import com.example.auth.security.JwtTokenProvider;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

@WebMvcTest(DraftAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class DraftAdminControllerErrorTest {

    @Autowired MockMvc mockMvc;

    @MockBean ApprovalLineTemplateRepository approvalLineTemplateRepository;
    @MockBean DraftFormTemplateRepository draftFormTemplateRepository;
    @MockBean ApprovalGroupRepository approvalGroupRepository;
    @MockBean RequirePermissionAspect requirePermissionAspect;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("승인선 템플릿 조회 시 저장소 예외가 나면 500을 반환한다")
    void listApprovalTemplates_whenRepoFails_returns500() throws Exception {
        when(approvalLineTemplateRepository.findAll()).thenThrow(new com.example.file.FileStorageException("fail", null));

        mockMvc.perform(get("/api/admin/draft/templates/approval"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("양식 템플릿 조회가 빈 결과면 200/빈 배열을 반환한다")
    void listFormTemplates_returnsEmpty() throws Exception {
        when(draftFormTemplateRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/draft/templates/form"))
                .andExpect(status().isOk());
    }
}
