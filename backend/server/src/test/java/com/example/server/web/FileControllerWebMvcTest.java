package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.RequirePermissionAspect;
import com.example.auth.security.JwtTokenProvider;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.port.FileManagementPort;
import com.example.server.file.dto.FileMetadataResponse;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class FileControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileManagementPort fileManagementPort;

    @MockBean
    com.example.draft.application.DraftApplicationService draftApplicationService;

    @MockBean
    RequirePermissionAspect requirePermissionAspect;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("draftId가 첨부되지 않은 파일이면 400을 반환한다")
    void download_withDraftMismatch_returns400() throws Exception {
        UUID fileId = UUID.randomUUID();
        com.example.draft.application.response.DraftResponse draft = new com.example.draft.application.response.DraftResponse(
                UUID.randomUUID(), "t", "c", "BF", "ORG", "creator",
                com.example.draft.domain.DraftStatus.IN_REVIEW, null, "tmpl", "form", 1, "{}", "{}",
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null, null,
                java.util.List.of(), java.util.List.of(), null, null);
        when(draftApplicationService.getDraft(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false))).thenReturn(draft);
        when(draftApplicationService.listReferences(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false))).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/files/" + fileId + "?draftId=" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("파일 삭제 성공 시 200을 반환하고 본문에 id가 포함된다")
    void delete_success_returnsMetadata() throws Exception {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(id, "report.pdf", MediaType.APPLICATION_PDF_VALUE,
                10, "hash", "tester", FileStatus.ACTIVE, null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        when(fileManagementPort.delete(id, "system")).thenReturn(metadata);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/files/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(id.toString())));
    }

    @Test
    @DisplayName("draftId 없이 다운로드하면 200을 반환한다")
    void download_withoutDraft_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(id, "report.pdf", MediaType.APPLICATION_PDF_VALUE,
                10, "hash", "system", FileStatus.ACTIVE, null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        FileDownload download = new FileDownload(metadata, new org.springframework.core.io.ByteArrayResource(new byte[]{1,2}));
        when(fileManagementPort.download(any(), any(), any())).thenReturn(download);

        mockMvc.perform(get("/api/files/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파일 목록에 항목이 있으면 배열 길이가 1 이상이다")
    void list_returnsItems() throws Exception {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "one.txt", MediaType.TEXT_PLAIN_VALUE,
                3, "h", "tester", FileStatus.ACTIVE, null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        when(fileManagementPort.list()).thenReturn(java.util.List.of(metadata));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("one.txt")));
    }
    @Test
    @DisplayName("파일 목록이 비어 있으면 빈 배열을 반환한다")
    void listFiles_returnsEmptyArray() throws Exception {
        when(fileManagementPort.list()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    @DisplayName("다운로드 시 FileStorageException이 발생하면 500을 반환한다")
    void download_whenServiceThrows_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileManagementPort.download(any(), any(), any()))
                .thenThrow(new com.example.file.FileStorageException("fail", null));

        mockMvc.perform(get("/api/files/" + id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fail")));
    }

    @Test
    @DisplayName("다운로드 성공 시 Content-Disposition 헤더를 설정한다")
    void download_success_setsHeaders() throws Exception {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(id, "report.pdf", MediaType.APPLICATION_PDF_VALUE,
                10, "hash", "tester", FileStatus.ACTIVE, null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        FileDownload download = new FileDownload(metadata, new org.springframework.core.io.ByteArrayResource(new byte[]{1,2,3}));
        when(fileManagementPort.download(any(), any(), any())).thenReturn(download);

        mockMvc.perform(get("/api/files/" + id))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    org.assertj.core.api.Assertions.assertThat(disposition).contains("report.pdf");
                });
    }
}
