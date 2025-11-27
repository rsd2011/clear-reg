package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;
import com.example.file.dto.FileUploadCommand;

class FileServiceUploadMimeMismatchTest {

    @Test
    @DisplayName("MIME 검증이 엄격할 때 내용과 확장자가 다르면 업로드를 거부한다")
    void uploadFailsWhenMimeMismatch() {
        StoredFileRepository storedFileRepository = mock(StoredFileRepository.class);
        StoredFileVersionRepository versionRepository = mock(StoredFileVersionRepository.class);
        FileAccessLogRepository accessLogRepository = mock(FileAccessLogRepository.class);
        FileStorageClient storageClient = mock(FileStorageClient.class);
        PolicySettingsProvider policySettingsProvider = mock(PolicySettingsProvider.class);
        FileScanner fileScanner = mock(FileScanner.class);
        FileAuditPublisher auditPublisher = mock(FileAuditPublisher.class);
        FileSecurityProperties securityProperties = new FileSecurityProperties();
        Clock clock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);

        given(policySettingsProvider.currentSettings()).willReturn(
                new PolicyToggleSettings(false, false, false, List.of(), 10_000, List.of("pdf"), true, 30));

        FileService service = new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, clock);

        FileUploadCommand command = new FileUploadCommand(
                "report.pdf",
                "application/pdf",
                5,
                () -> new ByteArrayInputStream("plain-text".getBytes()),
                OffsetDateTime.now(clock).plusDays(1),
                "user");

        assertThatThrownBy(() -> service.upload(command))
                .isInstanceOf(FilePolicyViolationException.class)
                .hasMessageContaining("확장자가 일치");
    }
}
