package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.example.file.port.FileManagementPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController 테스트")
class FileControllerTest {

    @Mock
    private FileManagementPort fileManagementPort;

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
        given(fileManagementPort.download(metadata.id(), "tester"))
                .willReturn(new FileDownload(metadata, new ByteArrayResource("data".getBytes())));

        var response = controller.download(metadata.id());

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
}
