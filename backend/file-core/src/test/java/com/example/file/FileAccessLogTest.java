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
        OffsetDateTime now = OffsetDateTime.now();
        StoredFile file = StoredFile.create("file.txt", null, "owner", null, "owner", now);
        FileAccessLog log = FileAccessLog.recordAccess(file, "DOWNLOAD", "user", "ok", now);

        assertThat(log.getFile()).isEqualTo(file);
        assertThat(log.getActor()).isEqualTo("user");
        assertThat(log.getAction()).isEqualTo("DOWNLOAD");
        assertThat(log.getDetail()).isEqualTo("ok");
        assertThat(log.getCreatedAt()).isEqualTo(now);
    }
}
