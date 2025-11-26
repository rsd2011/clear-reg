package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.file.config.FileSecurityProperties;
import com.example.common.policy.PolicySettingsProvider;
import org.springframework.beans.factory.ObjectProvider;
import com.example.file.storage.FileStorageClient;
import com.example.file.port.FileScanner;

@ExtendWith(MockitoExtension.class)
class FileScanReschedulerTest {

    @Mock
    StoredFileRepository storedFileRepository;

    @Mock
    StoredFileVersionRepository versionRepository;

    @Mock
    FileStorageClient storageClient;

    @Mock
    FileScanner fileScanner;

    @Mock
    StoredFileVersion version;

    @Mock
    org.springframework.core.io.Resource resource;

    @Test
    @DisplayName("재스캔이 비활성화되면 어떤 저장소도 호출하지 않는다")
    void skipWhenRescanDisabled() {
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setRescanEnabled(false);
        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, nullProvider(), false);

        rescheduler.rescanPending();

        verify(storedFileRepository, never()).findTop20ByScanStatusInOrderByCreatedAtAsc(List.of(ScanStatus.PENDING, ScanStatus.FAILED));
        assertThat(properties.isRescanEnabled()).isFalse();
    }

    @Test
    @DisplayName("재스캔이 활성화되면 PENDING 파일을 스캔하고 상태를 갱신한다")
    void rescanHappyPathUpdatesStatus() throws Exception {
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setRescanEnabled(true);
        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, nullProvider(), false);

        StoredFile file = StoredFile.create("doc.txt", null, "owner", null, "owner", java.time.OffsetDateTime.now());
        file.markScanResult(ScanStatus.PENDING, java.time.OffsetDateTime.now(), "old");
        when(storedFileRepository.findTop20ByScanStatusInOrderByCreatedAtAsc(List.of(ScanStatus.PENDING, ScanStatus.FAILED)))
                .thenReturn(List.of(file));
        when(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(any()))
                .thenReturn(java.util.Optional.of(version));
        when(storageClient.load(any())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("data".getBytes()));
        when(fileScanner.scan(any(), any())).thenReturn(ScanStatus.CLEAN);

        rescheduler.rescanPending();

        assertThat(file.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        assertThat(file.getBlockedReason()).isNull();
        verify(storedFileRepository).saveAll(List.of(file));
    }

    @Test
    @DisplayName("재스캔 결과가 BLOCKED이면 blockedReason을 설정한다")
    void rescanBlockedSetsReason() throws Exception {
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setRescanEnabled(true);
        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, nullProvider(), false);

        StoredFile file = StoredFile.create("doc.txt", null, "owner", null, "owner", java.time.OffsetDateTime.now());
        file.markScanResult(ScanStatus.PENDING, java.time.OffsetDateTime.now(), null);

        when(storedFileRepository.findTop20ByScanStatusInOrderByCreatedAtAsc(List.of(ScanStatus.PENDING, ScanStatus.FAILED)))
                .thenReturn(List.of(file));
        when(versionRepository.findFirstByFileIdOrderByVersionNumberDesc(any()))
                .thenReturn(java.util.Optional.of(version));
        when(storageClient.load(any())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("data".getBytes()));
        when(fileScanner.scan(any(), any())).thenReturn(ScanStatus.BLOCKED);

        rescheduler.rescanPending();

        assertThat(file.getScanStatus()).isEqualTo(ScanStatus.BLOCKED);
        assertThat(file.getBlockedReason()).contains("Rescan result");
    }

    @Test
    @DisplayName("central scheduler가 활성화되면 @Scheduled 메서드는 실행을 건너뛴다")
    void skipWhenCentralSchedulerEnabled() {
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setRescanEnabled(true);
        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, nullProvider(), true);

        rescheduler.rescanPending();

        verify(storedFileRepository, never()).findTop20ByScanStatusInOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("trigger 디스크립터를 생성한다")
    void triggerNotNull() {
        FileSecurityProperties properties = new FileSecurityProperties();
        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, nullProvider(), false);

        assertThat(rescheduler.trigger()).isNotNull();
    }

    @Test
    @DisplayName("정책 스케줄이 있으면 그것을 사용한다")
    void triggerUsesPolicySchedule() {
        FileSecurityProperties properties = new FileSecurityProperties();
        var policy = mock(PolicySettingsProvider.class);
        var provider = new ObjectProvider<PolicySettingsProvider>() {
            @Override public PolicySettingsProvider getObject(Object... args) { return policy; }
            @Override public PolicySettingsProvider getIfAvailable() { return policy; }
            @Override public PolicySettingsProvider getIfUnique() { return policy; }
            @Override public PolicySettingsProvider getObject() { return policy; }
            @Override public java.util.stream.Stream<PolicySettingsProvider> stream() { return java.util.stream.Stream.of(policy); }
            @Override public java.util.stream.Stream<PolicySettingsProvider> orderedStream() { return java.util.stream.Stream.of(policy); }
        };
        when(policy.batchJobSchedule(com.example.common.schedule.BatchJobCode.FILE_SECURITY_RESCAN))
                .thenReturn(new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, 1234, 0, null));

        FileScanRescheduler rescheduler = new FileScanRescheduler(
                storedFileRepository, versionRepository, fileScanner, storageClient, properties, provider, false);

        assertThat(rescheduler.trigger().toString()).contains("1234");
    }

    private ObjectProvider<PolicySettingsProvider> nullProvider() {
        return new ObjectProvider<>() {
            @Override public PolicySettingsProvider getObject(Object... args) { return null; }
            @Override public PolicySettingsProvider getIfAvailable() { return null; }
            @Override public PolicySettingsProvider getIfUnique() { return null; }
            @Override public PolicySettingsProvider getObject() { return null; }
            @Override public java.util.stream.Stream<PolicySettingsProvider> stream() { return java.util.stream.Stream.empty(); }
            @Override public java.util.stream.Stream<PolicySettingsProvider> orderedStream() { return java.util.stream.Stream.empty(); }
        };
    }
}
