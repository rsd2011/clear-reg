package com.example.admin.systemconfig.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemConfigNotFoundException 테스트")
class SystemConfigNotFoundExceptionTest {

  @Test
  @DisplayName("Given: 메시지, When: 예외 생성, Then: 메시지 포함")
  void shouldCreateExceptionWithMessage() {
    // Given
    String message = "설정을 찾을 수 없습니다: auth.settings";

    // When
    SystemConfigNotFoundException exception = new SystemConfigNotFoundException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
  }

  @Test
  @DisplayName("Given: 메시지와 원인, When: 예외 생성, Then: 메시지와 원인 포함")
  void shouldCreateExceptionWithMessageAndCause() {
    // Given
    String message = "설정을 찾을 수 없습니다: auth.settings";
    Throwable cause = new IllegalArgumentException("잘못된 설정 코드");

    // When
    SystemConfigNotFoundException exception = new SystemConfigNotFoundException(message, cause);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getCause().getMessage()).isEqualTo("잘못된 설정 코드");
  }
}
