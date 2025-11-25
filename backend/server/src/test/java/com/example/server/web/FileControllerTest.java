package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.common.security.RowScope;
import com.example.server.file.FileMetadataResponse;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.response.DraftAttachmentResponse;
import com.example.draft.application.response.DraftApprovalStepResponse;
import com.example.draft.application.response.DraftResponse;
import com.example.file.port.FileManagementPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController 테스트")
class FileControllerTest {

    @Mock
    private FileManagementPort fileManagementPort;

    @Mock
    private DraftApplicationService draftApplicationService;

    @InjectMocks
    private FileController controller;

    @BeforeEach
    void setUp() {
        AuthContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private void setAuth(ActionCode action) {
        AuthContextHolder.set(new AuthContext("tester", "ORG", "DEFAULT",
                FeatureCode.FILE, action, RowScope.ALL, java.util.Map.of()));
    }

    @Test
    @DisplayName("Given 업로드 요청 When 호출하면 Then 서비스에 위임하고 메타데이터를 반환한다")
    void givenUpload_whenPosting_thenDelegateToService() throws Exception {
        setAuth(ActionCode.UPLOAD);
        FileMetadataDto metadata = sampleMetadata();
        given(fileManagementPort.upload(any())).willReturn(metadata);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "content".getBytes());

        FileMetadataResponse response = controller.uploadFile(multipartFile, null);

        assertThat(response.originalName()).isEqualTo("hello.txt");
        verify(fileManagementPort).upload(any());
    }

    @Test
    @DisplayName("Given 다운로드 요청 When 호출하면 Then 리소스를 반환한다")
    void givenDownload_whenRequesting_thenReturnResource() {
        setAuth(ActionCode.DOWNLOAD);
        FileMetadataDto metadata = sampleMetadata();
        given(fileManagementPort.download(metadata.id(), "tester", List.of("tester")))
                .willReturn(new FileDownload(metadata, new ByteArrayResource("data".getBytes())));

        var response = controller.download(metadata.id(), null);

        assertThat(response.getHeaders().getContentDisposition().getFilename()).contains("hello.txt");
    }

    @Test
    @DisplayName("Given 목록 요청 When 호출하면 Then 파일 메타데이터 목록을 반환한다")
    void givenList_whenRequested_thenReturnMetadata() {
        setAuth(ActionCode.READ);
        given(fileManagementPort.list()).willReturn(List.of(sampleMetadata()));

        java.util.List<FileMetadataResponse> response = controller.listFiles();

        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("파일이 없을 때 다운로드하면 예외를 전파한다")
    void downloadThrowsWhenServiceFails() {
        setAuth(ActionCode.DOWNLOAD);
        UUID id = sampleMetadata().id();
        given(fileManagementPort.download(id, "tester", List.of("tester"))).willThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> controller.download(id, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("파일 삭제 시 서비스 예외를 전파한다")
    void deleteThrowsWhenServiceFails() {
        setAuth(ActionCode.DELETE);
        UUID id = UUID.randomUUID();
        given(fileManagementPort.delete(id, "tester")).willThrow(new IllegalStateException("fail"));

        assertThatThrownBy(() -> controller.delete(id))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("목록이 비어 있으면 빈 리스트를 반환한다")
    void listReturnsEmpty() {
        setAuth(ActionCode.READ);
        given(fileManagementPort.list()).willReturn(List.of());

        var response = controller.listFiles();

        assertThat(response).isEmpty();
    }

    private FileMetadataDto sampleMetadata() {
        OffsetDateTime ts = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new FileMetadataDto(
                UUID.randomUUID(),
                "hello.txt",
                "text/plain",
                4,
                "abcd",
                "tester",
                FileStatus.ACTIVE,
                null,
                ts,
                ts);
    }

    @Test
    @DisplayName("draftId 첨부 불일치 시 정책 위반 예외를 던진다")
    void downloadWithDraftId_whenNotAttached_throwsPolicyViolation() {
        setAuth(ActionCode.DOWNLOAD);
        UUID fileId = sampleMetadata().id();
        DraftResponse draft = new DraftResponse(UUID.randomUUID(), "t", "c", "BF", "ORG",
                "creator", com.example.draft.domain.DraftStatus.IN_REVIEW, null, "tmpl", "form", 1,
                "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null, null,
                List.<DraftApprovalStepResponse>of(), List.of(new DraftAttachmentResponse(UUID.randomUUID(), "other", "name", 1L, java.time.OffsetDateTime.now(), "creator")),
                null, null);
        given(draftApplicationService.getDraft(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false))).willReturn(draft);
        given(draftApplicationService.listReferences(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false))).willReturn(List.of());

        assertThatThrownBy(() -> controller.download(fileId, UUID.randomUUID()))
                .isInstanceOf(com.example.file.FilePolicyViolationException.class)
                .hasMessageContaining("첨부되지 않은 파일");
    }

    @Test
    @DisplayName("파일 삭제 성공 시 메타데이터를 반환한다")
    void deleteReturnsMetadata() {
        setAuth(ActionCode.DELETE);
        FileMetadataDto metadata = sampleMetadata();
        given(fileManagementPort.delete(metadata.id(), "tester")).willReturn(metadata);

        FileMetadataResponse response = controller.delete(metadata.id());

        assertThat(response.id()).isEqualTo(metadata.id());
    }

    @Test
    @DisplayName("draftId가 있고 첨부가 일치하면 다운로드를 허용한다")
    void downloadWithDraftId_whenAttached_returnsResource() {
        setAuth(ActionCode.DOWNLOAD);
        FileMetadataDto metadata = sampleMetadata();
        UUID draftId = UUID.randomUUID();
        DraftResponse draft = new DraftResponse(draftId, "t", "c", "BF", "ORG",
                "tester", com.example.draft.domain.DraftStatus.IN_REVIEW, null, "tmpl", "form", 1,
                "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null, null,
                List.<DraftApprovalStepResponse>of(), List.of(new DraftAttachmentResponse(metadata.id(), "name", "name", 1L, java.time.OffsetDateTime.now(), "tester")),
                null, null);
        given(draftApplicationService.getDraft(eq(draftId), any(), any(), eq(false))).willReturn(draft);
        given(draftApplicationService.listReferences(eq(draftId), any(), any(), eq(false))).willReturn(List.of());
        given(fileManagementPort.download(eq(metadata.id()), any(), any()))
                .willReturn(new FileDownload(metadata, new ByteArrayResource("data".getBytes())));

        var resp = controller.download(metadata.id(), draftId);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
