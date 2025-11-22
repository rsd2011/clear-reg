package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceVersionMissingTest {

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

    FileServiceVersionMissingTest() {
        given(policySettingsProvider.currentSettings()).willReturn(
                new PolicyToggleSettings(false, false, false, java.util.List.of(), 0, java.util.List.of(), true, 30));
        service = new FileService(storedFileRepository, versionRepository, accessLogRepository, storageClient,
                policySettingsProvider, fileScanner, auditPublisher, securityProperties, clock);
    }

    @Test
    @DisplayName("버전 정보를 찾지 못하면 StoredFileNotFoundException을 던진다")
    void download_throws_whenVersionMissing() {
        UUID id = UUID.randomUUID();
        StoredFile file = new StoredFile();
        file.setOwnerUsername("owner");
        file.setStatus(FileStatus.ACTIVE);
        file.setScanStatus(ScanStatus.CLEAN);
        file.markCreated("owner", OffsetDateTime.now(clock));

        given(storedFileRepository.findById(id)).willReturn(Optional.of(file));
        given(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(file.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id, "owner", java.util.List.of("owner")))
                .isInstanceOf(StoredFileNotFoundException.class);
    }
}
