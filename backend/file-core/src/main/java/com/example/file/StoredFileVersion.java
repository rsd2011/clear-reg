package com.example.file;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_file_versions")
public class StoredFileVersion extends PrimaryKeyEntity {

    protected StoredFileVersion() {
    }

    private StoredFileVersion(int versionNumber,
                              String storagePath,
                              String checksum,
                              String createdBy,
                              OffsetDateTime createdAt) {
        this.versionNumber = versionNumber;
        this.storagePath = storagePath;
        this.checksum = checksum;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static StoredFileVersion createVersion(int versionNumber,
                                                  String storagePath,
                                                  String checksum,
                                                  String createdBy,
                                                  OffsetDateTime createdAt) {
        if (storagePath == null || checksum == null || createdBy == null || createdAt == null) {
            throw new IllegalArgumentException("storagePath, checksum, createdBy, createdAt must be provided");
        }
        return new StoredFileVersion(versionNumber, storagePath, checksum, createdBy, createdAt);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    public StoredFile getFile() {
        return file;
    }

    void setFile(StoredFile file) {
        this.file = file;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getChecksum() {
        return checksum;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
