package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RowConditionEvaluatorBlankTest {

    RowConditionEvaluator evaluator = new RowConditionEvaluator();

    @Test
    @DisplayName("expression이 null 또는 공백이면 무조건 true를 반환한다")
    void blankExpressionReturnsTrue() {
        assertThat(evaluator.isAllowed(null, Map.of("k", 1))).isTrue();
        assertThat(evaluator.isAllowed("   ", Map.of())).isTrue();
    }
}
