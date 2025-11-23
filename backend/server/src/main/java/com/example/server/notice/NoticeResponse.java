package com.example.server.notice;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        return from(notice, UnaryOperator.identity());
    }

    public static NoticeResponse from(Notice notice, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NoticeResponse(
                notice.getId(),
                fn.apply(notice.getDisplayNumber()),
                fn.apply(notice.getTitle()),
                fn.apply(notice.getContent()),
                notice.getSeverity(),
                notice.getAudience(),
                notice.isPinned(),
                notice.getPublishAt(),
                notice.getExpireAt());
    }

    public static NoticeResponse apply(NoticeResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new NoticeResponse(
                response.id(),
                fn.apply(response.displayNumber()),
                fn.apply(response.title()),
                fn.apply(response.content()),
                response.severity(),
                response.audience(),
                response.pinned(),
                response.publishAt(),
                response.expireAt());
    }
}
