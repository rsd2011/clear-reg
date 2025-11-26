package com.example.file;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.common.file.FileStatus;
import com.example.common.jpa.PrimaryKeyEntity;
import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stored_files")
@Getter
public class StoredFile extends PrimaryKeyEntity {

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long size;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "owner_username", nullable = false, length = 100)
    private String ownerUsername;

    @Column(name = "sha256", length = 128)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 20)
    private ScanStatus scanStatus = ScanStatus.PENDING;

    @Column(name = "scanned_at")
    private OffsetDateTime scannedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status = FileStatus.ACTIVE;

    @Column(name = "retention_until")
    private OffsetDateTime retentionUntil;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_accessed_at")
    private OffsetDateTime lastAccessedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Version
    private long version;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Getter(AccessLevel.NONE)
    private List<StoredFileVersion> versions = new ArrayList<>();

    public List<StoredFileVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public static StoredFile create(String originalName,
                                    String contentType,
                                    String ownerUsername,
                                    OffsetDateTime retentionUntil,
                                    String actor,
                                    OffsetDateTime now) {
        if (originalName == null || ownerUsername == null || actor == null || now == null) {
            throw new IllegalArgumentException("originalName, ownerUsername, actor, now 는 필수입니다");
        }
        StoredFile file = new StoredFile();
        file.originalName = originalName;
        file.contentType = contentType;
        file.ownerUsername = ownerUsername;
        file.retentionUntil = retentionUntil;
        file.status = FileStatus.ACTIVE;
        file.scanStatus = ScanStatus.PENDING;
        file.markCreated(actor, now);
        return file;
    }

    void addVersion(StoredFileVersion version) {
        versions.add(version);
        version.setFile(this);
    }

    public boolean isDeleted() {
        return status == FileStatus.DELETED;
    }

    public void markAccessed(OffsetDateTime now) {
        this.lastAccessedAt = now;
    }

    public void updateHashes(long size, String checksum, String sha256) {
        this.size = size;
        this.checksum = checksum;
        this.sha256 = sha256;
    }

    public void markScanResult(ScanStatus scanStatus, OffsetDateTime scannedAt, String blockedReason) {
        this.scanStatus = scanStatus;
        this.scannedAt = scannedAt;
        this.blockedReason = blockedReason;
    }

    public void changeRetentionUntil(OffsetDateTime retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public void markDeleted(String actor, OffsetDateTime now) {
        this.status = FileStatus.DELETED;
        markUpdated(actor, now);
    }

    public void markCreated(String actor, OffsetDateTime now) {
        this.createdBy = actor;
        this.updatedBy = actor;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markUpdated(String actor, OffsetDateTime now) {
        this.updatedBy = actor;
        this.updatedAt = now;
    }
}
