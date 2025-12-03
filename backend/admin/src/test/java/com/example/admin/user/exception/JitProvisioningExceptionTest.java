package com.example.admin.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JitProvisioningException 테스트")
class JitProvisioningExceptionTest {

  @Test
  @DisplayName("Given 메시지 When 생성 Then 메시지 설정됨")
  void givenMessage_whenConstruct_thenMessageSet() {
    // Given
    String message = "JIT 프로비저닝 실패";

    // When
    JitProvisioningException exception = new JitProvisioningException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
  }

  @Test
  @DisplayName("Given 메시지와 원인 When 생성 Then 메시지와 원인 설정됨")
  void givenMessageAndCause_whenConstruct_thenMessageAndCauseSet() {
    // Given
    String message = "JIT 프로비저닝 실패";
    Throwable cause = new RuntimeException("원인 예외");

    // When
    JitProvisioningException exception = new JitProvisioningException(message, cause);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }
}
