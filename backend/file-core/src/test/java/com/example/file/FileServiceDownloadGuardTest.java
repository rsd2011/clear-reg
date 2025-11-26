package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
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

import com.example.common.file.FileDownload;
import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

@DisplayName("FileService 다운로드 가드 시나리오")
class FileServiceDownloadGuardTest {

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

    private StoredFile baseFile() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        StoredFile file = StoredFile.create("file.txt", null, "owner", null, "owner", now);
        file.markScanResult(ScanStatus.CLEAN, now, null);
        return file;
    }

    @Test
    @DisplayName("삭제된 파일이면 404를 던진다")
    void deletedFileThrowsNotFound() {
        StoredFile file = baseFile();
        file.markDeleted("actor", OffsetDateTime.now(clock));
        UUID id = file.getId();
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));

        assertThatThrownBy(() -> service().download(id, "owner"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    @DisplayName("소유자가 아닌 사용자는 권한 예외가 발생한다")
    void nonOwnerWithoutDelegationThrowsPolicyViolation() {
        StoredFile file = baseFile();
        UUID id = file.getId();
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));

        assertThatThrownBy(() -> service().download(id, "intruder"))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    @Test
    @DisplayName("스캔이 BLOCKED면 다운로드를 차단한다")
    void blockedFileThrowsPolicyViolation() {
        StoredFile file = baseFile();
        file.markScanResult(ScanStatus.BLOCKED, OffsetDateTime.now(clock), "blocked");
        UUID id = file.getId();
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));

        assertThatThrownBy(() -> service().download(id, "owner"))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    @Test
    @DisplayName("위임된 사용자는 스캔이 완료된 경우 다운로드할 수 있다")
    void delegatedUserCanDownload() throws Exception {
        StoredFile file = baseFile();
        UUID id = UUID.randomUUID();
        StoredFileVersion version = StoredFileVersion.createVersion(
                1,
                "path",
                "chk",
                "owner",
                OffsetDateTime.now(clock)
        );
        file.addVersion(version);
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));
        when(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(version));
        when(storageClient.load("path")).thenReturn(new InputStreamResource(new ByteArrayInputStream("ok".getBytes())));

        FileDownload download = service().download(id, "delegate", List.of("delegate"));

        assertThat(download.metadata().ownerUsername()).isEqualTo("owner");
    }

    @Test
    @DisplayName("스캔이 완료되지 않았으면 다운로드를 차단한다")
    void pendingScanBlocksDownload() {
        StoredFile file = baseFile();
        file.markScanResult(ScanStatus.PENDING, OffsetDateTime.now(clock), null);
        UUID id = UUID.randomUUID();
        when(storedFileRepository.findById(id)).thenReturn(java.util.Optional.of(file));

        assertThatThrownBy(() -> service().download(id, "owner"))
                .isInstanceOf(FilePolicyViolationException.class);
    }
}
