package com.example.server.notice.dto;

import com.example.server.notice.Notice;
import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeSeverity;
import com.example.server.notice.NoticeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        return from(notice, UnaryOperator.identity());
    }

    public static NoticeAdminResponse from(Notice notice, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NoticeAdminResponse(
                notice.getId(),
                fn.apply(notice.getDisplayNumber()),
                fn.apply(notice.getTitle()),
                fn.apply(notice.getContent()),
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

    public static NoticeAdminResponse apply(NoticeAdminResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NoticeAdminResponse(
                response.id(),
                fn.apply(response.displayNumber()),
                fn.apply(response.title()),
                fn.apply(response.content()),
                response.severity(),
                response.audience(),
                response.status(),
                response.pinned(),
                response.publishAt(),
                response.expireAt(),
                response.createdAt(),
                response.updatedAt(),
                response.createdBy(),
                response.updatedBy()
        );
    }
}
