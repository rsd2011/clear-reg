package com.example.server.notice;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoticeAdminResponse(UUID id,
                                  String displayNumber,
                                  String title,
                                  String content,
                                  NoticeSeverity severity,
                                  NoticeAudience audience,
                                  NoticeStatus status,
                                  boolean pinned,
                                  OffsetDateTime publishAt,
                                  OffsetDateTime expireAt,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt,
                                  String createdBy,
                                  String updatedBy) {

    public static NoticeAdminResponse from(Notice notice) {
        return new NoticeAdminResponse(
                notice.getId(),
                notice.getDisplayNumber(),
                notice.getTitle(),
                notice.getContent(),
                notice.getSeverity(),
                notice.getAudience(),
                notice.getStatus(),
                notice.isPinned(),
                notice.getPublishAt(),
                notice.getExpireAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                notice.getCreatedBy(),
                notice.getUpdatedBy());
    }
}
