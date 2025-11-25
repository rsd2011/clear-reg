package com.example.auth.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.common.security.RowScope;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextTaskDecoratorTest {

  private final AuthContextTaskDecorator decorator = new AuthContextTaskDecorator();

  @AfterEach
  void tearDown() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("TaskDecorator가 AuthContext를 비동기 실행기로 전달한다")
  void decoratorPropagatesContext() {
    AuthContext context =
        new AuthContext(
            "async-user",
            "ORG",
            "DEFAULT",
            FeatureCode.FILE,
            ActionCode.DOWNLOAD,
            RowScope.OWN,
            java.util.Map.of());
    AuthContextHolder.set(context);
    AtomicReference<AuthContext> observed = new AtomicReference<>();

    Runnable decorated =
        decorator.decorate(() -> observed.set(AuthContextHolder.current().orElse(null)));

    AuthContextHolder.clear();
    decorated.run();

    assertThat(observed.get()).isEqualTo(context);
    assertThat(AuthContextHolder.current()).isEmpty();
  }
}
