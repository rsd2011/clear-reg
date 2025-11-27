package com.example.draft.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.DraftAttachment;

public record DraftAttachmentResponse(UUID fileId,
                                      String fileName,
                                      String contentType,
                                      long fileSize,
                                      OffsetDateTime attachedAt,
                                      String attachedBy) {

    public static DraftAttachmentResponse from(DraftAttachment attachment) {
        return new DraftAttachmentResponse(
                attachment.getStoredFileId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getAttachedAt(),
                attachment.getAttachedBy()
        );
    }
}
