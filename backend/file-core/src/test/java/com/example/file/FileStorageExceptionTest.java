package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileStorageExceptionTest {

    @Test
    @DisplayName("cause 생성자는 원인 예외를 보존한다")
    void causeConstructor() {
        RuntimeException cause = new RuntimeException("root");
        FileStorageException ex = new FileStorageException("with cause", cause);
        assertThat(ex).hasMessageContaining("with cause");
        assertThat(ex).hasCause(cause);
    }

    @Test
    @DisplayName("null cause로도 생성 가능하며 메시지를 보존한다")
    void nullCauseStillStoresMessage() {
        FileStorageException ex = new FileStorageException("storage error", null);
        assertThat(ex).hasMessage("storage error");
    }
}
