package com.example.file.api.dto;

import java.time.OffsetDateTime;

public record FileUploadRequest(OffsetDateTime retentionUntil) {
}
