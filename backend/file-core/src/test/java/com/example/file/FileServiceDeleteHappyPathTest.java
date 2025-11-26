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

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDeleteHappyPathTest {

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
    @DisplayName("삭제 시 상태가 DELETED로 변경된다")
    void delete_marksDeleted() {
        UUID id = UUID.randomUUID();
        StoredFile file = StoredFile.create("ok.txt", null, "owner", null, "owner", OffsetDateTime.now(clock));
        given(storedFileRepository.findById(id)).willReturn(Optional.of(file));
        given(storedFileRepository.save(Mockito.any())).willAnswer(inv -> inv.getArgument(0));

        StoredFile deleted = service.delete(id, "owner");

        assertThat(deleted.getStatus()).isEqualTo(FileStatus.DELETED);
    }
}
