package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RowConditionEvaluatorTrueTest {

  RowConditionEvaluator evaluator = new RowConditionEvaluator();

  @Test
  @DisplayName("조건이 충족되면 true를 반환한다")
  void returnsTrueWhenExpressionMatches() {
    boolean allowed = evaluator.isAllowed("#root['age'] >= 18", Map.of("age", 20));
    assertThat(allowed).isTrue();
  }
}
