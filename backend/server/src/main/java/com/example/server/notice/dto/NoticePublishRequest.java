package com.example.server.notice.dto;

import java.time.OffsetDateTime;

public record NoticePublishRequest(OffsetDateTime publishAt,
                                   OffsetDateTime expireAt,
                                   Boolean pinned) {
}
