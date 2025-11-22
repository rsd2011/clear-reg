package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RowConditionEvaluatorFalseTest {

    RowConditionEvaluator evaluator = new RowConditionEvaluator();

    @Test
    @DisplayName("조건이 존재하지만 속성이 없으면 false를 반환한다")
    void returnsFalseWhenAttributeMissing() {
        boolean allowed = evaluator.isAllowed("#age > 10", Map.of("name", "bob"));
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("잘못된 SpEL은 IllegalArgumentException을 던진다")
    void invalidExpressionThrows() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.isAllowed("{{", Map.of()));
    }
}
