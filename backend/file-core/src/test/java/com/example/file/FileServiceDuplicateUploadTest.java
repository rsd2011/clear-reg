package com.example.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

class FileServiceDuplicateUploadTest {

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
        given(policySettingsProvider.currentSettings()).willReturn(settings);
        return new FileService(storedFileRepository, versionRepository, accessLogRepository,
                storageClient, policySettingsProvider, fileScanner, auditPublisher, securityProperties, fixedClock);
    }

    @Test
    @DisplayName("동일 이름의 활성 파일이 존재하면 업로드를 거부한다")
    void rejectDuplicateActiveFile() {
        FileSummaryView summary = mock(FileSummaryView.class);
        given(summary.getOriginalName()).willReturn("dup.txt");
        given(summary.getStatus()).willReturn(FileStatus.ACTIVE);
        given(storedFileRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(summary));

        FileUploadCommand command = new FileUploadCommand(
                "dup.txt",
                "text/plain",
                4,
                () -> new ByteArrayInputStream("data".getBytes()),
                null,
                "user"
        );

        assertThatThrownBy(() -> service().upload(command))
                .isInstanceOf(FilePolicyViolationException.class)
                .hasMessageContaining("덮어쓸 수 없습니다");

        try {
            verify(storageClient, never()).store(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        } catch (Exception ignored) {
            // Mockito signature declares throws Exception
        }
    }
}
