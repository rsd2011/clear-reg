package com.example.server.drm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoDeletingFileResourceTest {

    @Test
    @DisplayName("AutoDeletingFileResource는 스트림 close 시 임시 파일을 삭제한다")
    void deletesOnClose() throws Exception {
        Path temp = Files.createTempFile("auto-del", ".txt");
        Files.writeString(temp, "hello");

        AutoDeletingFileResource res = new AutoDeletingFileResource(temp);
        try (InputStream is = res.getInputStream()) {
            assertThat(is.read()).isEqualTo('h');
        }
        assertThat(Files.exists(temp)).isFalse();

        // 두 번째 close 호출에도 예외 없이 동작
        res.close();
        assertThat(Files.exists(temp)).isFalse();
    }
}
