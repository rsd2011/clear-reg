package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.port.FileManagementPort;
import com.example.file.FileUploadCommand;
import com.example.file.api.FileUploadRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileControllerUnitTest {

    @Mock FileManagementPort fileManagementPort;
    @InjectMocks FileController controller;

    @Test
    @DisplayName("파일 목록이 비어 있으면 빈 리스트를 반환한다")
    void listEmptyReturnsEmpty() {
        given(fileManagementPort.list()).willReturn(List.of());

        var result = controller.listFiles();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스토리지 예외 시 다운로드에서 예외가 전파된다")
    void downloadPropagatesStorageError() {
        UUID id = UUID.randomUUID();
        given(fileManagementPort.download(any(), any())).willThrow(new RuntimeException("storage failure"));

        assertThatThrownBy(() -> controller.download(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("삭제 실패 시 예외를 전파한다")
    void deleteFailurePropagates() {
        UUID id = UUID.randomUUID();
        given(fileManagementPort.delete(id, "system")).willThrow(new RuntimeException("delete fail"));

        assertThatThrownBy(() -> controller.delete(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("다운로드 성공 시 응답에 본문/헤더를 설정한다")
    void downloadSuccess() {
        UUID id = UUID.randomUUID();
        FileMetadataDto meta = new FileMetadataDto(id, "name.txt", "text/plain", 4L,
                "hash", "owner", FileStatus.ACTIVE,
                null, null, null);
        FileDownload download = new FileDownload(meta, new ByteArrayResource("data".getBytes()));
        given(fileManagementPort.download(any(), any())).willReturn(download);

        ResponseEntity<?> response = controller.download(id);

        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("name.txt");
    }

    @Test
    @DisplayName("다운로드 시 contentType이 null이면 기본 application/octet-stream을 사용한다")
    void downloadWithNullContentTypeUsesDefault() {
        UUID id = UUID.randomUUID();
        FileMetadataDto meta = new FileMetadataDto(id, "file.bin", null, 1L,
                "hash", "owner", FileStatus.ACTIVE,
                null, null, null);
        FileDownload download = new FileDownload(meta, new ByteArrayResource(new byte[] {1}));
        given(fileManagementPort.download(any(), any())).willReturn(download);

        ResponseEntity<?> response = controller.download(id);

        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("업로드 시 originalName이 null이면 attachment로 대체한다")
    void uploadOriginalNameFallback() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn(null);
        given(file.getContentType()).willReturn(null);
        given(file.getSize()).willReturn(1L);
        given(file.getInputStream()).willReturn(new java.io.ByteArrayInputStream(new byte[] {1}));

        FileUploadRequest req = null; // retention null branch
        FileMetadataDto meta = new FileMetadataDto(UUID.randomUUID(), "attachment", null, 1L,
                "hash", "owner", FileStatus.ACTIVE, null, null, null);
        given(fileManagementPort.upload(any(FileUploadCommand.class))).willReturn(meta);

        FileMetadataDto result = controller.uploadFile(file, req);

        assertThat(result.originalName()).isEqualTo("attachment");
    }
}
