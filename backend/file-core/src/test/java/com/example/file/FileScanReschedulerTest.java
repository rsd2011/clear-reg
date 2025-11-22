package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.file.config.FileSecurityProperties;
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
                storedFileRepository, versionRepository, fileScanner, storageClient, properties);

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
                storedFileRepository, versionRepository, fileScanner, storageClient, properties);

        StoredFile file = new StoredFile();
        file.setOriginalName("doc.txt");
        file.setScanStatus(ScanStatus.PENDING);
        file.setBlockedReason("old");
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
                storedFileRepository, versionRepository, fileScanner, storageClient, properties);

        StoredFile file = new StoredFile();
        file.setOriginalName("doc.txt");
        file.setScanStatus(ScanStatus.PENDING);

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
}
