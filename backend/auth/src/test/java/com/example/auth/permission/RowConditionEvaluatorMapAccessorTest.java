package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;

class RowConditionEvaluatorMapAccessorTest {

    RowConditionEvaluator.MapPropertyAccessor accessor = new RowConditionEvaluator.MapPropertyAccessor();

    @Test
    @DisplayName("MapPropertyAccessor는 존재하지 않는 키는 읽지 못한다")
    void cannotReadMissingKey() throws Exception {
        boolean canRead = accessor.canRead(null, Map.of("name", "alice"), "age");
        assertThat(canRead).isFalse();
    }

    @Test
    @DisplayName("MapPropertyAccessor는 Map이 아닌 객체는 거부한다")
    void readNonMapThrows() {
        assertThrows(AccessException.class, () -> accessor.read(null, new Object(), "any"));
    }

    @Test
    @DisplayName("MapPropertyAccessor는 값이 있을 때 TypedValue로 반환한다")
    void readExistingKey() throws Exception {
        TypedValue value = accessor.read(null, Map.of("name", "alice"), "name");
        assertThat(value.getValue()).isEqualTo("alice");
    }
}
