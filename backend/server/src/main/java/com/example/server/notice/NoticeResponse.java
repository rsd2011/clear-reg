package com.example.server.notice;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoticeResponse(UUID id,
                             String displayNumber,
                             String title,
                             String content,
                             NoticeSeverity severity,
                             NoticeAudience audience,
                             boolean pinned,
                             OffsetDateTime publishAt,
                             OffsetDateTime expireAt) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getDisplayNumber(),
                notice.getTitle(),
                notice.getContent(),
                notice.getSeverity(),
                notice.getAudience(),
                notice.isPinned(),
                notice.getPublishAt(),
                notice.getExpireAt());
    }
}
