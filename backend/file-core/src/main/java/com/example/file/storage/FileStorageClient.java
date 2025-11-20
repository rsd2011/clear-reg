package com.example.file.storage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface FileStorageClient {

    StoredObject store(InputStream inputStream, long size, String suggestedName) throws IOException;

    Resource load(String storagePath) throws IOException;

    void delete(String storagePath) throws IOException;

    record StoredObject(String storagePath, long size) {
    }
}
