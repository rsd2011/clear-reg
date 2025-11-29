package com.example.admin.rowaccesspolicy.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RowAccessPolicyRootNotFoundException 테스트")
class RowAccessPolicyRootNotFoundExceptionTest {

    @Test
    @DisplayName("메시지만 포함하는 생성자")
    void constructorWithMessage() {
        // given
        String message = "정책을 찾을 수 없습니다.";

        // when
        RowAccessPolicyRootNotFoundException exception = new RowAccessPolicyRootNotFoundException(message);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("메시지와 원인을 포함하는 생성자")
    void constructorWithMessageAndCause() {
        // given
        String message = "정책을 찾을 수 없습니다.";
        IllegalArgumentException cause = new IllegalArgumentException("원인 예외");

        // when
        RowAccessPolicyRootNotFoundException exception = new RowAccessPolicyRootNotFoundException(message, cause);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("원인 예외");
    }
}
