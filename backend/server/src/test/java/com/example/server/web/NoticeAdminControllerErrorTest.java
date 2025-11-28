package com.example.server.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.admin.permission.aop.RequirePermissionAspect;
import com.example.auth.security.JwtTokenProvider;
import com.example.server.notice.NoticeService;

@WebMvcTest(NoticeAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class NoticeAdminControllerErrorTest {

    @Autowired MockMvc mockMvc;

    @MockBean NoticeService noticeService;
    @MockBean RequirePermissionAspect requirePermissionAspect;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("공지 목록 조회 중 예외가 나면 500을 반환한다")
    void listNotices_whenServiceFails_returns500() throws Exception {
        when(noticeService.listNotices()).thenThrow(new com.example.file.FileStorageException("boom", null));

        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isInternalServerError());
    }

    // validation 제약이 없는 DTO이므로 생략
}
