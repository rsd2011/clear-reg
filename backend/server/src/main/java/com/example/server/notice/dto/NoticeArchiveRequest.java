package com.example.server.notice.dto;

import java.time.OffsetDateTime;

public record NoticeArchiveRequest(OffsetDateTime expireAt) {
}
