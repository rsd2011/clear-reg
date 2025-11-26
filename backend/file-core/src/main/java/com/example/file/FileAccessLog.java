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
@Table(name = "file_access_logs")
public class FileAccessLog extends PrimaryKeyEntity {

    protected FileAccessLog() {
    }

    private FileAccessLog(StoredFile file,
                          String action,
                          String actor,
                          String detail,
                          OffsetDateTime createdAt) {
        this.file = file;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public static FileAccessLog recordAccess(StoredFile file,
                                             String action,
                                             String actor,
                                             String detail,
                                             OffsetDateTime createdAt) {
        if (file == null || action == null || actor == null || createdAt == null) {
            throw new IllegalArgumentException("file, action, actor, createdAt must be provided");
        }
        return new FileAccessLog(file, action, actor, detail, createdAt);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public StoredFile getFile() {
        return file;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
