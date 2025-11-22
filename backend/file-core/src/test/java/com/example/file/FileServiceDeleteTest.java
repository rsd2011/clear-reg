package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDeleteTest {

    private final StoredFileRepository storedFileRepository = mock(StoredFileRepository.class);
    private final StoredFileVersionRepository versionRepository = mock(StoredFileVersionRepository.class);
    private final FileAccessLogRepository accessLogRepository = mock(FileAccessLogRepository.class);
    private final FileStorageClient storageClient = mock(FileStorageClient.class);
    private final PolicySettingsProvider policySettingsProvider = mock(PolicySettingsProvider.class);
    private final FileScanner fileScanner = mock(FileScanner.class);
    private final FileAuditPublisher auditPublisher = mock(FileAuditPublisher.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
    private final FileSecurityProperties securityProperties = new FileSecurityProperties();

    private FileService service() {
        var settings = new PolicyToggleSettings(false, false, false, List.of(), 20, List.of("txt"), true, 365);
        org.mockito.Mockito.when(policySettingsProvider.currentSettings()).thenReturn(settings);
        return new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, fixedClock);
    }

    @Test
    @DisplayName("삭제 대상이 없으면 StoredFileNotFoundException을 던진다")
    void deleteThrowsWhenFileMissing() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().delete(id, "user"))
                .isInstanceOf(StoredFileNotFoundException.class);

        verify(storageClient, never()).delete(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("이미 삭제된 파일은 저장소 삭제를 시도하지 않고 그대로 반환한다")
    void deleteSkipsWhenAlreadyDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        StoredFile file = new StoredFile();
        file.setStatus(FileStatus.DELETED);
        org.mockito.Mockito.when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));

        StoredFile result = service().delete(id, "user");

        verify(storageClient, never()).delete(org.mockito.Mockito.any());
        org.assertj.core.api.Assertions.assertThat(result.getStatus()).isEqualTo(FileStatus.DELETED);
    }

    @Test
    @DisplayName("스토리지 삭제 중 IOException이 발생해도 파일 상태를 DELETED로 저장한다")
    void deleteIgnoresStorageDeleteFailures() throws Exception {
        UUID id = UUID.randomUUID();
        StoredFile file = new StoredFile();
        StoredFileVersion version = new StoredFileVersion();
        version.setStoragePath("path-1");
        version.setVersionNumber(1);
        version.setCreatedAt(OffsetDateTime.now(fixedClock));
        file.addVersion(version);
        org.mockito.Mockito.when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));
        org.mockito.Mockito.when(storedFileRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(file);
        org.mockito.Mockito.doThrow(new java.io.IOException("fs error")).when(storageClient).delete("path-1");

        StoredFile deleted = service().delete(id, "user");

        org.assertj.core.api.Assertions.assertThat(deleted.getStatus()).isEqualTo(FileStatus.DELETED);
        verify(storageClient).delete("path-1");
        verify(storedFileRepository).save(org.mockito.ArgumentMatchers.any());
    }
}
