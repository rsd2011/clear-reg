package com.example.file.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageClient implements FileStorageClient {

    private final FileStorageProperties properties;

    public LocalFileStorageClient(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObject store(InputStream inputStream, long size, String suggestedName) throws IOException {
        Path root = Path.of(properties.getRootPath());
        Files.createDirectories(root);
        String datePath = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path directory = root.resolve(datePath);
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + "-" + sanitizeFileName(suggestedName);
        Path target = directory.resolve(filename);
        long written = Files.copy(inputStream, target);
        return new StoredObject(root.relativize(target).toString(), written);
    }

    @Override
    public Resource load(String storagePath) throws IOException {
        Path root = Path.of(properties.getRootPath());
        Path path = root.resolve(storagePath);
        if (!Files.exists(path)) {
            throw new IOException("파일을 찾을 수 없습니다: " + storagePath);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path root = Path.of(properties.getRootPath());
        Path path = root.resolve(storagePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    private String sanitizeFileName(String name) {
        return name == null ? "attachment" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
