package com.example.admin.approval.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalGroupNotFoundExceptionTest {

    @Test
    @DisplayName("메시지만으로 예외를 생성한다")
    void createWithMessage() {
        ApprovalGroupNotFoundException ex = new ApprovalGroupNotFoundException("not found");

        assertThat(ex.getMessage()).isEqualTo("not found");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("메시지와 원인으로 예외를 생성한다")
    void createWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ApprovalGroupNotFoundException ex = new ApprovalGroupNotFoundException("not found", cause);

        assertThat(ex.getMessage()).isEqualTo("not found");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
