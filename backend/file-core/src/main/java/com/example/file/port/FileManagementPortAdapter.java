package com.example.file.port;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.common.file.FileDownload;
import com.example.common.file.dto.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.FileService;
import com.example.file.FileSummaryView;
import com.example.file.dto.FileUploadCommand;
import com.example.file.StoredFile;
import com.example.file.audit.FileAuditEvent;
import com.example.file.audit.FileAuditPublisher;

@Component
public class FileManagementPortAdapter implements FileManagementPort {

    private final FileService fileService;
    private final FileAuditPublisher fileAuditPublisher;

    public FileManagementPortAdapter(FileService fileService, FileAuditPublisher fileAuditPublisher) {
        this.fileService = fileService;
        this.fileAuditPublisher = fileAuditPublisher;
    }

    @Override
    public FileMetadataDto upload(FileUploadCommand command) {
        FileMetadataDto metadata = toMetadata(fileService.upload(command));
        fileAuditPublisher.publish(new FileAuditEvent("UPLOAD", metadata.id(), command.ownerUsername(), metadata.createdAt()));
        return metadata;
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
    public FileDownload download(UUID id, String actor, List<String> alsoAllowedUsers) {
        FileDownload download = fileService.download(id, actor, alsoAllowedUsers);
        fileAuditPublisher.publish(new FileAuditEvent("DOWNLOAD", id, actor, download.metadata().updatedAt() != null ? download.metadata().updatedAt() : download.metadata().createdAt()));
        return download;
    }

    @Override
    public FileMetadataDto delete(UUID id, String actor) {
        FileMetadataDto metadata = toMetadata(fileService.delete(id, actor));
        fileAuditPublisher.publish(new FileAuditEvent("DELETE", id, actor, metadata.updatedAt() != null ? metadata.updatedAt() : metadata.createdAt()));
        return metadata;
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
