package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;

import com.example.common.file.FileStatus;
import com.example.common.file.FileDownload;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDownloadAllowedMinimalTest {

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
                new PolicyToggleSettings(false, false, false, java.util.List.of(), 0, java.util.List.of(), true, 30));
        service = new FileService(storedFileRepository, versionRepository, accessLogRepository, storageClient,
                policySettingsProvider, fileScanner, auditPublisher, securityProperties, clock);
    }

    @Test
    @DisplayName("CLEAN 상태이고 actor가 허용 목록이면 다운로드가 성공한다")
    void download_allowed_success() throws Exception {
        UUID id = UUID.randomUUID();
        StoredFile file = StoredFile.create("ok.txt", null, "owner", null, "owner", OffsetDateTime.now(clock));
        file.markScanResult(ScanStatus.CLEAN, OffsetDateTime.now(clock), null);

        StoredFileVersion version = StoredFileVersion.createVersion(
                1,
                "path",
                "chk",
                "owner",
                OffsetDateTime.now(clock)
        );
        file.addVersion(version);

        given(storedFileRepository.findById(id)).willReturn(Optional.of(file));
        given(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(Mockito.any(UUID.class)))
                .willReturn(Optional.of(version));
        given(storageClient.load("path")).willReturn(new ByteArrayResource("hi".getBytes()));

        FileDownload download = service.download(id, "owner", java.util.List.of("owner"));

        assertThat(download.metadata().originalName()).isEqualTo("ok.txt");
    }
}
