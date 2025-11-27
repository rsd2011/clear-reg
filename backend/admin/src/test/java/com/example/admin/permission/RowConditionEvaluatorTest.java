package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RowConditionEvaluatorTest {

  RowConditionEvaluator evaluator = new RowConditionEvaluator();

  @Test
  @DisplayName("속성이 일치하지 않으면 false를 반환한다")
  void returnsFalseWhenAttributeMissing() {
    boolean allowed = evaluator.isAllowed("team == 'OPS'", Map.of("team", "DEV"));

    assertThat(allowed).isFalse();
  }
}
