package com.example.server.notice;

import java.time.OffsetDateTime;

public record NoticePublishRequest(OffsetDateTime publishAt,
                                   OffsetDateTime expireAt,
                                   Boolean pinned) {
}
