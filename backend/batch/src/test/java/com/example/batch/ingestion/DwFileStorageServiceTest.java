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

import com.example.dw.config.DwIngestionProperties;

class DwFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private DwFileStorageService storageService;
    private DwIngestionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DwIngestionProperties();
        properties.setIncomingDir(tempDir.resolve("incoming"));
        properties.setArchiveDir(tempDir.resolve("archive"));
        properties.setErrorDir(tempDir.resolve("error"));
        storageService = new DwFileStorageService(properties);
    }

    @Test
    void givenPendingFiles_whenNextPendingFile_thenReturnSortedDescriptor() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Files.writeString(incoming.resolve("employee_20240102_002.csv"), "payload2");
        Files.writeString(incoming.resolve("employee_20240101_001.csv"), "payload1");

        Optional<?> descriptor = storageService.nextPendingFile();

        assertThat(descriptor).isPresent();
        assertThat(storageService.readPayload((com.example.dw.dto.HrFileDescriptor) descriptor.get())).isEqualTo("payload1");
    }

    @Test
    void givenProcessedFile_whenMarkedSuccess_thenMovesToArchive() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Path source = incoming.resolve("employee_20240101_001.csv");
        Files.writeString(source, "payload");
        var descriptor = new com.example.dw.dto.HrFileDescriptor("employee_20240101_001.csv",
                LocalDate.of(2024, 1, 1), 1, source, com.example.dw.dto.DataFeedType.EMPLOYEE);

        storageService.markProcessed(descriptor, true);

        assertThat(properties.getArchiveDir().resolve(source.getFileName())).exists();
    }

    @Test
    void givenHolidayFile_whenParsing_thenDescriptorContainsAttributes() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Path source = incoming.resolve("holiday_us_20240101_001.csv");
        Files.writeString(source, "20240101,US,New Year,New Year,false");

        Optional<com.example.dw.dto.HrFileDescriptor> descriptor = storageService.nextPendingFile();

        assertThat(descriptor).isPresent();
        assertThat(descriptor.get().feedType()).isEqualTo(com.example.dw.dto.DataFeedType.HOLIDAY);
        assertThat(descriptor.get().attributes().get("countryCode")).isEqualTo("US");
    }

    @Test
    void givenCommonCodeFile_whenParsing_thenDescriptorContainsCodeType() throws IOException {
        Path incoming = properties.getIncomingDir();
        Files.createDirectories(incoming);
        Path source = incoming.resolve("code_status_20240101_005.csv");
        Files.writeString(source, "header");

        Optional<com.example.dw.dto.HrFileDescriptor> descriptor = storageService.nextPendingFile();

        assertThat(descriptor).isPresent();
        assertThat(descriptor.get().feedType()).isEqualTo(com.example.dw.dto.DataFeedType.COMMON_CODE);
        assertThat(descriptor.get().attributes().get("codeType")).isEqualTo("STATUS");
    }
}
