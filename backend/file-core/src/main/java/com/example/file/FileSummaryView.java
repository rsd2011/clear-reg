package com.example.file;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.file.FileStatus;

public interface FileSummaryView {
    UUID getId();
    String getOriginalName();
    String getContentType();
    long getSize();
    String getOwnerUsername();
    FileStatus getStatus();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getUpdatedAt();
}
