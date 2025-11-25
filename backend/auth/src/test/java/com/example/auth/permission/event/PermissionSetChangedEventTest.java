package com.example.auth.permission.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionSetChangedEventTest {

  @Test
  @DisplayName("principalId가 null이면 전체 권한 변경 이벤트로 간주한다")
  void nullPrincipalIndicatesGlobalChange() {
    PermissionSetChangedEvent event = new PermissionSetChangedEvent(null);

    assertThat(event.principalId()).isNull();
  }

  @Test
  @DisplayName("principalId가 있으면 해당 사용자 변경 이벤트다")
  void principalPresentIndicatesUserChange() {
    PermissionSetChangedEvent event = new PermissionSetChangedEvent("user-1");

    assertThat(event.principalId()).isEqualTo("user-1");
  }
}
