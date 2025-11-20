package com.example.draft.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftAttachment extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private Draft draft;

    @Column(name = "stored_file_id", nullable = false, columnDefinition = "uuid")
    private UUID storedFileId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "attached_by", nullable = false, length = 100)
    private String attachedBy;

    @Column(name = "attached_at", nullable = false)
    private OffsetDateTime attachedAt;

    private DraftAttachment(UUID storedFileId,
                            String fileName,
                            String contentType,
                            long fileSize,
                            String attachedBy,
                            OffsetDateTime attachedAt) {
        this.storedFileId = storedFileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.attachedBy = attachedBy;
        this.attachedAt = attachedAt;
    }

    public static DraftAttachment create(UUID storedFileId,
                                         String fileName,
                                         String contentType,
                                         long fileSize,
                                         String attachedBy,
                                         OffsetDateTime attachedAt) {
        return new DraftAttachment(storedFileId, fileName, contentType, fileSize, attachedBy, attachedAt);
    }

    void attachTo(Draft draft) {
        this.draft = draft;
    }
}
