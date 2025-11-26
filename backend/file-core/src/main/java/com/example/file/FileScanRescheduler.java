package com.example.file;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;
import com.example.file.config.FileSecurityProperties;
import com.example.file.port.FileScanner;
import com.example.file.storage.FileStorageClient;

@Component
public class FileScanRescheduler implements ScheduledJobPort {

    private final StoredFileRepository storedFileRepository;
    private final StoredFileVersionRepository versionRepository;
    private final FileScanner fileScanner;
    private final FileStorageClient storageClient;
    private final FileSecurityProperties securityProperties;
    private final PolicySettingsProvider policySettingsProvider;
    private final boolean centralSchedulerEnabled;

    public FileScanRescheduler(StoredFileRepository storedFileRepository,
                               StoredFileVersionRepository versionRepository,
                               FileScanner fileScanner,
                               FileStorageClient storageClient,
                               FileSecurityProperties securityProperties,
                               ObjectProvider<PolicySettingsProvider> policySettingsProvider,
                               @Value("${central.scheduler.enabled:false}") boolean centralSchedulerEnabled) {
        this.storedFileRepository = storedFileRepository;
        this.versionRepository = versionRepository;
        this.fileScanner = fileScanner;
        this.storageClient = storageClient;
        this.securityProperties = securityProperties;
        this.policySettingsProvider = policySettingsProvider.getIfAvailable();
        this.centralSchedulerEnabled = centralSchedulerEnabled;
    }


    @Scheduled(fixedDelayString = "${file.security.rescan-interval-ms:60000}")
    @Transactional
    public void rescanPending() {
        if (centralSchedulerEnabled) {
            return; // 중앙 스케줄러가 실행
        }
        runOnce(java.time.Instant.now());
    }

    @Override
    public String jobId() {
        return BatchJobCode.FILE_SECURITY_RESCAN.name();
    }

    @Override
    public TriggerDescriptor trigger() {
        BatchJobSchedule schedule = resolveSchedule();
        return schedule.toTriggerDescriptor();
    }

    @Override
    public void runOnce(java.time.Instant now) {
        if (!securityProperties.isRescanEnabled()) {
            return;
        }
        List<StoredFile> targets = storedFileRepository.findTop20ByScanStatusInOrderByCreatedAtAsc(
                List.of(ScanStatus.PENDING, ScanStatus.FAILED));
        OffsetDateTime nowTs = OffsetDateTime.now();
        for (StoredFile file : targets) {
            versionRepository.findFirstByFileIdOrderByVersionNumberDesc(file.getId()).ifPresent(version -> {
                try {
                    var resource = storageClient.load(version.getStoragePath());
                    try (InputStream is = resource.getInputStream()) {
                        ScanStatus status = fileScanner.scan(file.getOriginalName(), is);
                        String reason = (status == ScanStatus.BLOCKED || status == ScanStatus.FAILED)
                                ? "Rescan result: " + status : null;
                        file.markScanResult(status, nowTs, reason);
                    }
                } catch (IOException e) {
                    file.markScanResult(ScanStatus.FAILED, nowTs, "Rescan IO error: " + e.getMessage());
                }
            });
        }
        storedFileRepository.saveAll(targets);
    }

    private BatchJobSchedule resolveSchedule() {
        BatchJobSchedule policy = policySettingsProvider == null ? null
                : policySettingsProvider.batchJobSchedule(BatchJobCode.FILE_SECURITY_RESCAN);
        if (policy != null) {
            return policy;
        }
        long interval = securityProperties.getRescanIntervalMs() > 0
                ? securityProperties.getRescanIntervalMs()
                : BatchJobDefaults.defaults().get(BatchJobCode.FILE_SECURITY_RESCAN).fixedDelayMillis();
        boolean enabled = securityProperties.isRescanEnabled();
        return new BatchJobSchedule(enabled, com.example.common.schedule.TriggerType.FIXED_DELAY, null,
                interval, 0, null);
    }
}
