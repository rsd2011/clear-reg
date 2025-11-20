package com.example.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

@DisplayName("LocalFileStorageClient 테스트")
class LocalFileStorageClientTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageClient client;

    @BeforeEach
    void setUp() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRootPath(tempDir.toString());
        client = new LocalFileStorageClient(properties);
    }

    @Test
    @DisplayName("Given 업로드 스트림 When store 호출 Then 파일을 생성하고 메타데이터를 반환한다")
    void givenUploadStream_whenStore_thenCreateFileAndReturnMetadata() throws Exception {
        byte[] payload = "file-payload".getBytes(StandardCharsets.UTF_8);

        FileStorageClient.StoredObject stored = client.store(new ByteArrayInputStream(payload), payload.length, "report final.pdf");

        assertThat(stored.size()).isEqualTo(payload.length);
        assertThat(stored.storagePath()).contains("report_final.pdf");
        assertThat(stored.storagePath()).matches("\\d{4}/\\d{2}/\\d{2}/.*report_final\\.pdf$");
        assertThat(Files.exists(tempDir.resolve(stored.storagePath()))).isTrue();
    }

    @Test
    @DisplayName("Given 파일명이 null일 때 When store 호출 Then placeholder 이름을 사용한다")
    void givenNullFilename_whenStore_thenUsePlaceholder() throws Exception {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        FileStorageClient.StoredObject stored = client.store(new ByteArrayInputStream(payload), payload.length, null);

        assertThat(stored.storagePath()).contains("attachment");
    }

    @Test
    @DisplayName("Given 존재하는 파일 When load 호출 Then Resource를 반환한다")
    void givenExistingFile_whenLoad_thenReturnResource() throws Exception {
        Path existing = tempDir.resolve("existing.txt");
        Files.writeString(existing, "hello", StandardCharsets.UTF_8);

        Resource resource = client.load("existing.txt");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(5);
    }

    @Test
    @DisplayName("Given 존재하지 않는 파일 When load 호출 Then IOException이 발생한다")
    void givenMissingFile_whenLoad_thenThrowIOException() {
        assertThatThrownBy(() -> client.load("missing.bin"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Given 존재하는 파일 When delete 호출 Then 파일이 삭제된다")
    void givenExistingFile_whenDelete_thenRemoveFile() throws Exception {
        Path existing = tempDir.resolve("old.bin");
        Files.write(existing, "bye".getBytes(StandardCharsets.UTF_8));

        client.delete("old.bin");

        assertThat(Files.exists(existing)).isFalse();
    }

    @Test
    @DisplayName("Given 존재하지 않는 파일 When delete 호출 Then 예외 없이 종료된다")
    void givenMissingFile_whenDelete_thenNoException() {
        assertThatCode(() -> client.delete("not-there.txt")).doesNotThrowAnyException();
    }
}
