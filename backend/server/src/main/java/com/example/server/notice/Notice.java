package com.example.server.notice;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notices")
public class Notice extends PrimaryKeyEntity {

    protected Notice() {}

    @Column(name = "display_number", nullable = false, unique = true, length = 20)
    private String displayNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NoticeSeverity severity = NoticeSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 20)
    private NoticeAudience audience = NoticeAudience.GLOBAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NoticeStatus status = NoticeStatus.DRAFT;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "publish_at")
    private OffsetDateTime publishAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Version
    private long version;

    public String getDisplayNumber() { return displayNumber; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public NoticeSeverity getSeverity() { return severity; }
    public NoticeAudience getAudience() { return audience; }
    public NoticeStatus getStatus() { return status; }
    public boolean isPinned() { return pinned; }
    public OffsetDateTime getPublishAt() { return publishAt; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public long getVersion() { return version; }

    public boolean isArchived() {
        return status == NoticeStatus.ARCHIVED;
    }

    public boolean isPublished() {
        return status == NoticeStatus.PUBLISHED;
    }

    public static Notice createDraft(String displayNumber,
                                     String title,
                                     String content,
                                     NoticeSeverity severity,
                                     NoticeAudience audience,
                                     OffsetDateTime publishAt,
                                     OffsetDateTime expireAt,
                                     boolean pinned,
                                     String actor,
                                     OffsetDateTime now) {
        Notice n = new Notice();
        n.displayNumber = displayNumber;
        n.title = title;
        n.content = content;
        n.severity = severity == null ? NoticeSeverity.INFO : severity;
        n.audience = audience == null ? NoticeAudience.GLOBAL : audience;
        n.publishAt = publishAt;
        n.expireAt = expireAt;
        n.pinned = pinned;
        n.status = NoticeStatus.DRAFT;
        n.markCreated(actor, now);
        return n;
    }

    public void publish(OffsetDateTime publishAt, OffsetDateTime expireAt, boolean pinned, String actor, OffsetDateTime now) {
        if (isArchived()) throw new NoticeStateException("보관된 공지사항은 게시할 수 없습니다.");
        this.publishAt = publishAt != null ? publishAt : now;
        this.expireAt = expireAt;
        this.pinned = pinned;
        this.status = NoticeStatus.PUBLISHED;
        markUpdated(actor, now);
    }

    public void archive(OffsetDateTime expireAt, String actor, OffsetDateTime now) {
        this.status = NoticeStatus.ARCHIVED;
        this.pinned = false;
        this.expireAt = expireAt != null ? expireAt : now;
        markUpdated(actor, now);
    }

    public void updateContent(String title,
                              String content,
                              NoticeSeverity severity,
                              NoticeAudience audience,
                              OffsetDateTime publishAt,
                              OffsetDateTime expireAt,
                              boolean pinned,
                              String actor,
                              OffsetDateTime now) {
        if (isArchived()) throw new NoticeStateException("이미 보관된 공지사항은 수정할 수 없습니다.");
        this.title = title;
        this.content = content;
        this.severity = severity == null ? NoticeSeverity.INFO : severity;
        this.audience = audience == null ? NoticeAudience.GLOBAL : audience;
        this.publishAt = publishAt;
        this.expireAt = expireAt;
        this.pinned = pinned;
        markUpdated(actor, now);
    }

    public void pin(String actor, OffsetDateTime now) {
        this.pinned = true;
        markUpdated(actor, now);
    }

    public void unpin(String actor, OffsetDateTime now) {
        this.pinned = false;
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
