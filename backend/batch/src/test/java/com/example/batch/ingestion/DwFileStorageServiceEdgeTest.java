package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.config.DwIngestionProperties;

class DwFileStorageServiceEdgeTest {

    @Test
    @DisplayName("ingestion 비활성화 시 nextPendingFile은 empty를 반환한다")
    void disabledReturnsEmpty() {
        DwIngestionProperties properties = new DwIngestionProperties();
        properties.setEnabled(false);
        DwFileStorageService service = new DwFileStorageService(properties);

        assertThat(service.nextPendingFile()).isEmpty();
    }

    @Test
    @DisplayName("패턴에 맞지 않는 파일명은 무시된다")
    void ignoresUnexpectedFilename() throws Exception {
        DwIngestionProperties properties = new DwIngestionProperties();
        Path tempDir = Files.createTempDirectory("ingestion-test");
        properties.setIncomingDir(tempDir);
        Files.createFile(tempDir.resolve("unknown_20240101.csv"));

        DwFileStorageService service = new DwFileStorageService(properties);

        assertThat(service.nextPendingFile()).isEmpty();
    }
}
