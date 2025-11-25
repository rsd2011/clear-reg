package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RowConditionEvaluatorCacheHitTest {

  RowConditionEvaluator evaluator = new RowConditionEvaluator();

  @Test
  @DisplayName("같은 expression을 두 번 평가하면 캐시된 표현식을 재사용한다")
  void usesCompiledCache() {
    String expr = "#root['age'] > 10";
    Map<String, Object> attrs = Map.of("age", 20);

    boolean first = evaluator.isAllowed(expr, attrs);
    boolean second = evaluator.isAllowed(expr, attrs);

    assertThat(first).isTrue();
    assertThat(second).isTrue();
  }
}
