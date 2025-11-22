package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileAccessLogTest {

    @Test
    @DisplayName("FileAccessLog 필드 세터는 값을 보존한다")
    void settersPreserveValues() {
        FileAccessLog log = new FileAccessLog();
        StoredFile file = new StoredFile();
        file.setOriginalName("file.txt");
        log.setFile(file);
        log.setActor("user");
        log.setAction("DOWNLOAD");
        log.setDetail("ok");
        OffsetDateTime now = OffsetDateTime.now();
        log.setCreatedAt(now);

        assertThat(log.getFile()).isEqualTo(file);
        assertThat(log.getActor()).isEqualTo("user");
        assertThat(log.getAction()).isEqualTo("DOWNLOAD");
        assertThat(log.getDetail()).isEqualTo("ok");
        assertThat(log.getCreatedAt()).isEqualTo(now);
    }
}
