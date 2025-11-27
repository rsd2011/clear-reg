package com.example.server.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.PermissionEvaluator;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.PermissionDecision;
import com.example.admin.permission.RequirePermissionAspect;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionDeniedException;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.FileStorageException;
import com.example.file.StoredFileNotFoundException;
import com.example.file.storage.FileStorageClient;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.application.response.DraftApprovalStepResponse;
import com.example.draft.application.response.DraftAttachmentResponse;
import com.example.draft.domain.DraftStatus;
import com.example.file.FilePolicyViolationException;
import com.example.file.api.FileUploadRequest;
import com.example.file.port.FileManagementPort;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = FileController.class,
        properties = "spring.task.scheduling.enabled=false",
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FileController 통합 흐름(WebMvc)")
class FileFlowWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileManagementPort fileManagementPort;
    @MockBean
    DraftApplicationService draftApplicationService;
    @MockBean
    PermissionEvaluator permissionEvaluator;
    @MockBean
    RequirePermissionAspect requirePermissionAspect;
    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    private PermissionDecision allowDecision;
    private AuthContext authContext;

    @BeforeEach
    void setUpAuth() throws Throwable {
        authContext = new AuthContext("tester", "ORG", "PG", FeatureCode.FILE, ActionCode.READ, com.example.common.security.RowScope.ALL, null);
        allowDecision = org.mockito.Mockito.mock(PermissionDecision.class);
        org.mockito.BDDMockito.given(allowDecision.toContext()).willReturn(authContext);
        given(permissionEvaluator.evaluate(any(), any())).willReturn(allowDecision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));
        AuthContextHolder.set(authContext);
    }

    @AfterEach
    void clearAuth() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("업로드→목록→다운로드→삭제 흐름이 성공한다")
    void uploadListDownloadDelete() throws Exception {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(id, "hello.txt", "text/plain", 5L, "h",
                "tester", FileStatus.ACTIVE, null, null, null);
        given(fileManagementPort.upload(any())).willReturn(metadata);
        given(fileManagementPort.list()).willReturn(List.of(metadata));
        given(fileManagementPort.download(eq(id), any(), anyList()))
                .willReturn(new FileDownload(metadata, new org.springframework.core.io.ByteArrayResource("hi".getBytes(StandardCharsets.UTF_8))));
        given(fileManagementPort.delete(eq(id), eq("tester"))).willReturn(metadata);

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hi".getBytes());
        mockMvc.perform(multipart("/api/files").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));

        mockMvc.perform(get("/api/files/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("hello.txt")));

        mockMvc.perform(delete("/api/files/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("업로드 시 스토리지 클라이언트 오류가 발생하면 500을 반환한다")
    void upload_storageError_returns500() throws Exception {
        given(fileManagementPort.upload(any()))
                .willThrow(new FileStorageException("io", new IOException("fail")));

        MockMultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", "hi".getBytes());

        mockMvc.perform(multipart("/api/files").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("파일 목록이 비어 있으면 빈 배열을 반환한다")
    void list_empty_returnsEmptyArray() throws Exception {
        given(fileManagementPort.list()).willReturn(List.of());

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("파일명이 null이면 attachment로 대체되어 업로드가 성공한다")
    void upload_withoutOriginalName_usesAttachment() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", (String) null, "text/plain", "hi".getBytes());
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "attachment", "text/plain", 2L, "h",
                "tester", FileStatus.ACTIVE, null, null, null);
        given(fileManagementPort.upload(any())).willReturn(metadata);

        mockMvc.perform(multipart("/api/files").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("attachment"));
    }

    @Test
    @DisplayName("draftId 첨부 불일치 시 400을 반환한다")
    void downloadWithMismatchedDraft() throws Exception {
        UUID fileId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(fileId, "file.txt", "text/plain", 5L, "h",
                "tester", FileStatus.ACTIVE, null, null, null);
        given(fileManagementPort.upload(any())).willReturn(metadata);
        given(draftApplicationService.getDraft(draftId, "ORG", "tester", false))
                .willReturn(new DraftResponse(draftId, "t", "c", "BF", "ORG", "creator",
                        DraftStatus.DRAFT, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        List.<DraftApprovalStepResponse>of(), List.<DraftAttachmentResponse>of(), null, null));
        // 첨부되지 않으면 controller에서 download 호출 전에 정책 위반을 던지므로 download는 호출되지 않아도 됨
        given(fileManagementPort.download(eq(fileId), any(), anyList()))
                .willThrow(new FilePolicyViolationException("해당 기안에 첨부되지 않은 파일입니다."));

        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes());
        mockMvc.perform(multipart("/api/files").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/files/{id}", fileId).param("draftId", draftId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파일이 없으면 404를 반환한다")
    void download_notFound_returns404() throws Exception {
        UUID fileId = UUID.randomUUID();
        given(fileManagementPort.download(eq(fileId), any(), anyList()))
                .willThrow(new StoredFileNotFoundException(fileId));

        mockMvc.perform(get("/api/files/{id}", fileId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("삭제 중 스토리지 오류가 발생하면 500을 반환한다")
    void delete_failure_returns500() throws Exception {
        UUID fileId = UUID.randomUUID();
        given(fileManagementPort.delete(eq(fileId), eq("tester")))
                .willThrow(new FileStorageException("fail", new RuntimeException("x")));

        mockMvc.perform(delete("/api/files/{id}", fileId))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("다운로드 권한이 없으면 403을 반환한다")
    void download_forbidden_returns403() throws Exception {
        UUID fileId = UUID.randomUUID();
        given(fileManagementPort.download(eq(fileId), any(), anyList()))
                .willThrow(new PermissionDeniedException("denied"));

        mockMvc.perform(get("/api/files/{id}", fileId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("삭제 권한이 없으면 403을 반환한다")
    void delete_forbidden_returns403() throws Exception {
        UUID fileId = UUID.randomUUID();
        given(fileManagementPort.delete(eq(fileId), eq("tester")))
                .willThrow(new PermissionDeniedException("denied"));

        mockMvc.perform(delete("/api/files/{id}", fileId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("삭제 시 파일이 없으면 404를 반환한다")
    void delete_notFound_returns404() throws Exception {
        UUID fileId = UUID.randomUUID();
        given(fileManagementPort.delete(eq(fileId), eq("tester")))
                .willThrow(new StoredFileNotFoundException(fileId));

        mockMvc.perform(delete("/api/files/{id}", fileId))
                .andExpect(status().isNotFound());
    }
}
