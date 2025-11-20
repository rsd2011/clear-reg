package com.example.file.port;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.FileService;
import com.example.file.FileSummaryView;
import com.example.file.FileUploadCommand;
import com.example.file.StoredFile;

@Component
public class FileManagementPortAdapter implements FileManagementPort {

    private final FileService fileService;

    public FileManagementPortAdapter(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public FileMetadataDto upload(FileUploadCommand command) {
        return toMetadata(fileService.upload(command));
    }

    @Override
    public List<FileMetadataDto> list() {
        return fileService.listSummaries().stream()
                .map(this::fromSummary)
                .toList();
    }

    @Override
    public FileMetadataDto getMetadata(UUID id) {
        return toMetadata(fileService.getMetadata(id));
    }

    @Override
    public FileDownload download(UUID id, String actor) {
        return fileService.download(id, actor);
    }

    @Override
    public FileMetadataDto delete(UUID id, String actor) {
        return toMetadata(fileService.delete(id, actor));
    }

    private FileMetadataDto toMetadata(StoredFile file) {
        return new FileMetadataDto(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                file.getChecksum(),
                file.getOwnerUsername(),
                file.getStatus(),
                file.getRetentionUntil(),
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

    private FileMetadataDto fromSummary(FileSummaryView view) {
        return new FileMetadataDto(
                view.getId(),
                view.getOriginalName(),
                view.getContentType(),
                view.getSize(),
                null,
                view.getOwnerUsername(),
                view.getStatus() == null ? FileStatus.ACTIVE : view.getStatus(),
                null,
                view.getCreatedAt(),
                view.getUpdatedAt());
    }
}
