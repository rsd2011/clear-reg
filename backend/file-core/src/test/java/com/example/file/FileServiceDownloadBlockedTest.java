package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDownloadBlockedTest {

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
    @DisplayName("BLOCKED 상태 파일 다운로드 시 정책 위반 예외가 발생한다")
    void download_blockedFile_throws() {
        UUID id = UUID.randomUUID();
        StoredFile blocked = new StoredFile();
        blocked.setOriginalName("blocked.txt");
        blocked.setOwnerUsername("owner");
        blocked.setStatus(FileStatus.ACTIVE);
        blocked.setScanStatus(ScanStatus.BLOCKED);
        blocked.markCreated("owner", OffsetDateTime.now(clock));
        given(storedFileRepository.findById(id)).willReturn(Optional.of(blocked));

        assertThatThrownBy(() -> service.download(id, "other", java.util.List.of()))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    @Test
    @DisplayName("PENDING 상태 파일은 다운로드 시 정책 위반 예외가 발생한다")
    void download_pending_scan_throws() {
        UUID id = UUID.randomUUID();
        StoredFile pending = new StoredFile();
        pending.setOriginalName("pending.txt");
        pending.setOwnerUsername("owner");
        pending.setStatus(com.example.common.file.FileStatus.ACTIVE);
        pending.setScanStatus(ScanStatus.PENDING);
        pending.markCreated("owner", OffsetDateTime.now(clock));
        given(storedFileRepository.findById(id)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.download(id, "owner", java.util.List.of()))
                .isInstanceOf(FilePolicyViolationException.class);
    }
}
