package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextPropagatorRestoreTest {

  @Test
  @DisplayName("runWithContext는 이전 컨텍스트를 복원하고 새 컨텍스트를 적용한다")
  void runWithContextRestoresPrevious() {
    AuthContext previous =
        AuthContext.of("prev", "ORG", "PG", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN);
    AuthContextHolder.set(previous);

    AuthContext newCtx =
        AuthContext.of(
            "new",
            "ORG",
            "PG",
            FeatureCode.ORGANIZATION,
            ActionCode.UPDATE,
            RowScope.ALL);

    AuthContextPropagator.runWithContext(
        newCtx,
        () -> {
          assertThat(AuthContextHolder.current()).contains(newCtx);
        });

    assertThat(AuthContextHolder.current()).contains(previous);
    AuthContextHolder.clear();
  }
}
