package com.example.file.api;

import java.time.OffsetDateTime;

public record FileUploadRequest(OffsetDateTime retentionUntil) {
}
