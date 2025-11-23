package com.example.server.drm;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.lang.NonNull;

/**
 * 임시 파일을 Resource로 제공하면서 스트림이 닫히면 자동 삭제하는 리소스.
 * try-with-resources로 사용하면 close 시 deleteIfExists가 실행된다.
 */
public class AutoDeletingFileResource extends FileSystemResource implements AutoCloseable {

    private final Path path;

    public AutoDeletingFileResource(Path path) {
        super(path);
        this.path = path;
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        InputStream delegate = Files.newInputStream(path);
        return new FilterInputStream(delegate) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
