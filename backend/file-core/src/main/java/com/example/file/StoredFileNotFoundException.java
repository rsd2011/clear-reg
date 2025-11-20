package com.example.file;

import java.util.UUID;

public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(UUID id) {
        super("파일을 찾을 수 없습니다: " + id);
    }
}
