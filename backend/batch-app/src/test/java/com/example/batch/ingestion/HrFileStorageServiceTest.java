package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.hr.config.HrIngestionProperties;

class HrFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private HrFileStorageService storageService;
    private HrIngestionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new HrIngestionProperties();
        properties.setIncomingDir(tempDir.resolve("incoming"));
        properties.setArchiveDir(tempDir.resolve("archive"));
        properties.setErrorDir(tempDir.resolve("error"));
        storageService = new HrFileStorageService(properties);
    }

    @Test
    void givenPendingFiles_whenNextPendingFile_thenReturnSortedDescriptor() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Files.writeString(incoming.resolve("employee_20240102_002.csv"), "payload2");
        Files.writeString(incoming.resolve("employee_20240101_001.csv"), "payload1");

        Optional<?> descriptor = storageService.nextPendingFile();

        assertThat(descriptor).isPresent();
        assertThat(storageService.readPayload((com.example.hr.dto.HrFileDescriptor) descriptor.get())).isEqualTo("payload1");
    }

    @Test
    void givenProcessedFile_whenMarkedSuccess_thenMovesToArchive() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Path source = incoming.resolve("employee_20240101_001.csv");
        Files.writeString(source, "payload");
        var descriptor = new com.example.hr.dto.HrFileDescriptor("employee_20240101_001.csv",
                LocalDate.of(2024, 1, 1), 1, source, com.example.hr.dto.HrFeedType.EMPLOYEE);

        storageService.markProcessed(descriptor, true);

        assertThat(properties.getArchiveDir().resolve(source.getFileName())).exists();
    }
}
