package com.example.common.file;

import org.springframework.core.io.Resource;

import com.example.common.file.dto.FileMetadataDto;

public record FileDownload(FileMetadataDto metadata,
                           Resource resource) {
}
