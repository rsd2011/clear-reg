package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceFailureTest {

    StoredFileRepository storedFileRepository = Mockito.mock(StoredFileRepository.class);
    StoredFileVersionRepository versionRepository = Mockito.mock(StoredFileVersionRepository.class);
    FileAccessLogRepository accessLogRepository = Mockito.mock(FileAccessLogRepository.class);
    FileStorageClient storageClient = Mockito.mock(FileStorageClient.class);
    PolicySettingsProvider policySettingsProvider = Mockito.mock(PolicySettingsProvider.class);
    FileScanner fileScanner = Mockito.mock(FileScanner.class);
    FileAuditPublisher auditPublisher = Mockito.mock(FileAuditPublisher.class);
    FileSecurityProperties securityProperties = new FileSecurityProperties();
    Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);

    FileService service;

    @BeforeEach
    void setUp() {
        given(policySettingsProvider.currentSettings()).willReturn(
                new PolicyToggleSettings(false, false, false, List.of(), 0, List.of(), true, 30));
        service = new FileService(storedFileRepository, versionRepository, accessLogRepository, storageClient,
                policySettingsProvider, fileScanner, auditPublisher, securityProperties, clock);
    }

    @Test
    @DisplayName("업로드 중 스토리지 오류가 발생하면 FileStorageException을 던진다")
    void upload_storageError_throws() throws IOException {
        FileUploadCommand cmd = new FileUploadCommand("n", "text/plain", 1L,
                () -> { throw new RuntimeException(new IOException("fail")); }, null, "me");

        assertThatThrownBy(() -> service.upload(cmd))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("삭제 시 파일을 찾지 못하면 StoredFileNotFoundException을 던진다")
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        given(storedFileRepository.findById(id)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.delete(id, "actor"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }
}
