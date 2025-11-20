package com.example.file;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

@Component
public class FileScanRescheduler {

    private final StoredFileRepository storedFileRepository;
    private final StoredFileVersionRepository versionRepository;
    private final FileScanner fileScanner;
    private final FileStorageClient storageClient;
    private final FileSecurityProperties securityProperties;

    public FileScanRescheduler(StoredFileRepository storedFileRepository,
                               StoredFileVersionRepository versionRepository,
                               FileScanner fileScanner,
                               FileStorageClient storageClient,
                               FileSecurityProperties securityProperties) {
        this.storedFileRepository = storedFileRepository;
        this.versionRepository = versionRepository;
        this.fileScanner = fileScanner;
        this.storageClient = storageClient;
        this.securityProperties = securityProperties;
    }

    @Scheduled(fixedDelayString = "${file.security.rescan-interval-ms:60000}")
    @Transactional
    public void rescanPending() {
        if (!securityProperties.isRescanEnabled()) {
            return;
        }
        List<StoredFile> targets = storedFileRepository.findTop20ByScanStatusInOrderByCreatedAtAsc(
                List.of(ScanStatus.PENDING, ScanStatus.FAILED));
        OffsetDateTime now = OffsetDateTime.now();
        for (StoredFile file : targets) {
            versionRepository.findFirstByFileIdOrderByVersionNumberDesc(file.getId()).ifPresent(version -> {
                try {
                    var resource = storageClient.load(version.getStoragePath());
                    try (InputStream is = resource.getInputStream()) {
                        ScanStatus status = fileScanner.scan(file.getOriginalName(), is);
                        file.setScanStatus(status);
                        file.setScannedAt(now);
                        if (status == ScanStatus.BLOCKED || status == ScanStatus.FAILED) {
                            file.setBlockedReason("Rescan result: " + status);
                        } else {
                            file.setBlockedReason(null);
                        }
                    }
                } catch (IOException e) {
                    file.setScanStatus(ScanStatus.FAILED);
                    file.setBlockedReason("Rescan IO error: " + e.getMessage());
                }
            });
        }
        storedFileRepository.saveAll(targets);
    }
}
