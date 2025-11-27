package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.common.security.RowScope;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextPropagatorRestoreTest {

  @Test
  @DisplayName("runWithContext는 이전 컨텍스트를 복원하고 새 컨텍스트를 적용한다")
  void runWithContextRestoresPrevious() {
    AuthContext previous =
        new AuthContext(
            "prev", "ORG", "PG", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, Map.of());
    AuthContextHolder.set(previous);

    AuthContext newCtx =
        new AuthContext(
            "new",
            "ORG",
            "PG",
            FeatureCode.ORGANIZATION,
            ActionCode.UPDATE,
            RowScope.ALL,
            Map.of());

    AuthContextPropagator.runWithContext(
        newCtx,
        () -> {
          assertThat(AuthContextHolder.current()).contains(newCtx);
        });

    assertThat(AuthContextHolder.current()).contains(previous);
    AuthContextHolder.clear();
  }
}
