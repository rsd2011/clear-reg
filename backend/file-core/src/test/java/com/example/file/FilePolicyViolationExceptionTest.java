package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilePolicyViolationExceptionTest {

    @Test
    @DisplayName("메시지와 원인을 포함해 생성할 수 있다")
    void createsWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("inner");

        FilePolicyViolationException ex = new FilePolicyViolationException("rule broken", cause);

        assertThat(ex).hasMessage("rule broken").hasCause(cause);
    }
}
