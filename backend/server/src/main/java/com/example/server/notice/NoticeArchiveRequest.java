package com.example.server.notice;

import java.time.OffsetDateTime;

public record NoticeArchiveRequest(OffsetDateTime expireAt) {
}
