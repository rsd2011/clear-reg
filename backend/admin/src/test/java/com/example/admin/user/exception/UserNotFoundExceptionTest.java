package com.example.admin.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserNotFoundException 테스트")
class UserNotFoundExceptionTest {

  @Test
  @DisplayName("Given 사용자명 When 생성 Then 메시지와 username 설정됨")
  void givenUsername_whenConstruct_thenMessageAndUsernameSet() {
    // Given
    String username = "testuser";

    // When
    UserNotFoundException exception = new UserNotFoundException(username);

    // Then
    assertThat(exception.getMessage()).contains(username);
    assertThat(exception.getUsername()).isEqualTo(username);
  }
}
