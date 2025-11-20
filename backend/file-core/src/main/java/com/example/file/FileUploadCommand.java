package com.example.file;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Supplier;

public record FileUploadCommand(String originalName,
                                String contentType,
                                long size,
                                Supplier<InputStream> inputStreamSupplier,
                                OffsetDateTime retentionUntil,
                                String ownerUsername) {

    public FileUploadCommand {
        Objects.requireNonNull(originalName, "originalName is required");
        Objects.requireNonNull(inputStreamSupplier, "inputStreamSupplier is required");
        Objects.requireNonNull(ownerUsername, "ownerUsername is required");
    }
}
