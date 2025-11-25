package com.example.auth.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.common.security.RowScope;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextPropagatorTest {

  @AfterEach
  void tearDown() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("현재 AuthContext를 Runnable 로 전파한다")
  void runWithCurrentContext() {
    AuthContext context =
        new AuthContext(
            "tester",
            "ORG",
            "DEFAULT",
            FeatureCode.NOTICE,
            ActionCode.READ,
            RowScope.OWN,
            java.util.Map.of());
    AuthContextHolder.set(context);
    AtomicReference<AuthContext> observed = new AtomicReference<>();

    Runnable wrapped =
        AuthContextPropagator.wrapCurrentContext(
            () -> observed.set(AuthContextHolder.current().orElse(null)));

    AuthContextHolder.clear();
    wrapped.run();

    assertThat(observed.get()).isEqualTo(context);
    assertThat(AuthContextHolder.current()).isEmpty();
  }

  @Test
  @DisplayName("Callable에도 사용자 정의 컨텍스트를 적용 후 원래 상태를 복구한다")
  void callWithContext() throws Exception {
    AuthContext system =
        new AuthContext(
            "system",
            "ROOT",
            "AUDIT",
            FeatureCode.AUDIT_LOG,
            ActionCode.READ,
            RowScope.ALL,
            java.util.Map.of());
    AuthContextHolder.set(
        new AuthContext(
            "caller",
            "ORG",
            "DEFAULT",
            FeatureCode.NOTICE,
            ActionCode.READ,
            RowScope.OWN,
            java.util.Map.of()));

    Callable<String> callable =
        AuthContextPropagator.wrap(
            () -> AuthContextHolder.current().map(AuthContext::username).orElse("none"), system);

    String result = callable.call();

    assertThat(result).isEqualTo("system");
    assertThat(AuthContextHolder.current()).isPresent();
    assertThat(AuthContextHolder.current().map(AuthContext::username)).contains("caller");
  }
}
