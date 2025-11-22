package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
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
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;
import com.example.file.audit.FileAuditPublisher;

class FileServiceUploadTest {

    private final StoredFileRepository storedFileRepository = mock(StoredFileRepository.class);
    private final StoredFileVersionRepository versionRepository = mock(StoredFileVersionRepository.class);
    private final FileAccessLogRepository accessLogRepository = mock(FileAccessLogRepository.class);
    private final FileStorageClient storageClient = mock(FileStorageClient.class);
    private final PolicySettingsProvider policySettingsProvider = mock(PolicySettingsProvider.class);
    private final FileScanner fileScanner = mock(FileScanner.class);
    private final FileAuditPublisher auditPublisher = mock(FileAuditPublisher.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
    private final FileSecurityProperties securityProperties = new FileSecurityProperties();

    @Test
    @DisplayName("업로드 크기가 maxSizeBytes를 초과하면 업로드를 거부한다")
    void uploadFailsWhenExceedsMaxSize() {
        // Given
        securityProperties.setMaxSizeBytes(10);
        given(policySettingsProvider.currentSettings()).willReturn(
                new PolicyToggleSettings(false, false, false, List.of(), 10, List.of("txt"), true, 365)
        );

        FileService service = new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, fixedClock);

        FileUploadCommand command = new FileUploadCommand(
                "big.txt",
                "text/plain",
                11,
                () -> new ByteArrayInputStream("0123456789AB".getBytes()),
                null,
                "user"
        );

        // When // Then
        assertThatThrownBy(() -> service.upload(command))
                .isInstanceOf(FilePolicyViolationException.class)
                .hasMessageContaining("최대 파일 크기");
    }

    @Test
    @DisplayName("허용되지 않은 확장자면 업로드를 거부한다")
    void uploadFailsWhenExtensionNotAllowed() {
        securityProperties.setMaxSizeBytes(0); // 정책 기본값 사용
        PolicyToggleSettings settings = new PolicyToggleSettings(false, false, false, List.of(), 50, List.of("txt"), true, 365);
        given(policySettingsProvider.currentSettings()).willReturn(settings);

        FileService service = new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, fixedClock);

        FileUploadCommand command = new FileUploadCommand(
                "malware.exe",
                "application/octet-stream",
                4,
                () -> new ByteArrayInputStream("data".getBytes()),
                null,
                "user"
        );

        assertThatThrownBy(() -> service.upload(command))
                .isInstanceOf(FilePolicyViolationException.class)
                .hasMessageContaining("허용되지 않은 파일 확장자");
    }
}
