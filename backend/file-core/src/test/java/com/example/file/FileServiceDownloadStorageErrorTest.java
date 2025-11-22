package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDownloadStorageErrorTest {

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
    @DisplayName("스토리지가 IOException을 던지면 FileStorageException을 전파한다")
    void downloadFailsWhenStorageThrows() throws Exception {
        UUID id = UUID.randomUUID();
        StoredFile file = new StoredFile();
        file.setStatus(FileStatus.ACTIVE);
        file.setOwnerUsername("user");
        file.setScanStatus(ScanStatus.CLEAN);
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));
        StoredFileVersion version = new StoredFileVersion();
        version.setStoragePath("path");
        version.setVersionNumber(1);
        version.setCreatedAt(OffsetDateTime.now(clock));
        when(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(version));
        when(storageClient.load("path")).thenThrow(new IOException("io"));

        assertThatThrownBy(() -> service().download(id, "user"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("읽을 수 없습니다");
    }
}
