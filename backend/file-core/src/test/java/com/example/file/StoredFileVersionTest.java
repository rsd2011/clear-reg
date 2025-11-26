package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoredFileVersionTest {

    @Test
    @DisplayName("createVersion는 필드를 채우고 불변식을 검증한다")
    void createVersion_populatesFields() {
        OffsetDateTime now = OffsetDateTime.now();
        StoredFileVersion version = StoredFileVersion.createVersion(1, "path", "chk", "actor", now);

        assertThat(version.getVersionNumber()).isEqualTo(1);
        assertThat(version.getStoragePath()).isEqualTo("path");
        assertThat(version.getChecksum()).isEqualTo("chk");
        assertThat(version.getCreatedBy()).isEqualTo("actor");
        assertThat(version.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("필수값 누락 시 IllegalArgumentException을 던진다")
    void createVersion_requiresFields() {
        assertThatThrownBy(() -> StoredFileVersion.createVersion(1, null, "chk", "actor", OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
