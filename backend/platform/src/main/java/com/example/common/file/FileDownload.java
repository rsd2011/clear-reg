package com.example.common.file;

import org.springframework.core.io.Resource;

public record FileDownload(FileMetadataDto metadata,
                           Resource resource) {
}
