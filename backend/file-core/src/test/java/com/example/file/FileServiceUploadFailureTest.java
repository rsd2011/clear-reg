package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;
import com.example.file.dto.FileUploadCommand;

class FileServiceUploadFailureTest {

    StoredFileRepository storedFileRepository = Mockito.mock(StoredFileRepository.class);
    StoredFileVersionRepository versionRepository = Mockito.mock(StoredFileVersionRepository.class);
    FileAccessLogRepository accessLogRepository = Mockito.mock(FileAccessLogRepository.class);
    FileStorageClient storageClient = Mockito.mock(FileStorageClient.class);
    PolicySettingsProvider policySettingsProvider = Mockito.mock(PolicySettingsProvider.class);
    FileScanner fileScanner = Mockito.mock(FileScanner.class);
    FileAuditPublisher auditPublisher = Mockito.mock(FileAuditPublisher.class);
    FileSecurityProperties securityProperties = new FileSecurityProperties();
    Clock clock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);

    private FileService service() {
        when(policySettingsProvider.currentSettings()).thenReturn(
                new PolicyToggleSettings(false, false, false, List.of(), 100, List.of("txt"), true, 365)
        );
        return new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, clock);
    }

    @Test
    @DisplayName("스토리지 저장 실패 시 FileStorageException을 전파한다")
    void uploadFailsOnStorageError() throws Exception {
        FileUploadCommand command = new FileUploadCommand(
                "doc.txt", "text/plain", 4,
                () -> new java.io.ByteArrayInputStream("data".getBytes()),
                null, "user");
        when(storageClient.store(Mockito.any(), Mockito.anyLong(), Mockito.eq("doc.txt")))
                .thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> service().upload(command))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("저장");
    }
}
