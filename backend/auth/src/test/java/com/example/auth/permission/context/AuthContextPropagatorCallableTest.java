package com.example.auth.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextPropagatorCallableTest {

  @Test
  @DisplayName("wrapCurrentContext(Callable)는 현재 컨텍스트가 없어도 정상 실행한다")
  void wrapCurrentContextCallableRuns() throws Exception {
    var callable = AuthContextPropagator.wrapCurrentContext(() -> "ok");

    assertThat(callable.call()).isEqualTo("ok");
  }
}
