package com.example.file.port;

import java.util.List;
import java.util.UUID;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.file.FileUploadCommand;

public interface FileManagementPort {

    FileMetadataDto upload(FileUploadCommand command);

    List<FileMetadataDto> list();

    FileMetadataDto getMetadata(UUID id);

    FileDownload download(UUID id, String actor);

    FileMetadataDto delete(UUID id, String actor);
}
