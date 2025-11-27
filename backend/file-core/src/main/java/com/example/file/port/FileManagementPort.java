package com.example.file.port;

import java.util.List;
import java.util.UUID;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.file.dto.FileUploadCommand;

public interface FileManagementPort {

    FileMetadataDto upload(FileUploadCommand command);

    List<FileMetadataDto> list();

    FileMetadataDto getMetadata(UUID id);

    default FileDownload download(UUID id, String actor) {
        return download(id, actor, List.of());
    }

    FileDownload download(UUID id, String actor, List<String> alsoAllowedUsers);

    FileMetadataDto delete(UUID id, String actor);
}
