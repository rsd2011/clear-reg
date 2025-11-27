package com.example.server.notice.dto;

import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeSeverity;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotBlank
        @Size(max = 10_000)
        String content,
        @NotNull
        NoticeSeverity severity,
        NoticeAudience audience,
        OffsetDateTime publishAt,
        OffsetDateTime expireAt,
        Boolean pinned) {
}
