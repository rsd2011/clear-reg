package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.AccessException;

class RowConditionEvaluatorMapAccessorBranchTest {

  RowConditionEvaluator.MapPropertyAccessor accessor =
      new RowConditionEvaluator.MapPropertyAccessor();

  @Test
  @DisplayName("키가 존재하면 canRead가 true이고 read가 값을 반환한다")
  void canReadExistingKey() throws Exception {
    assertThat(accessor.canRead(null, Map.of("age", 20), "age")).isTrue();
    assertThat(accessor.read(null, Map.of("age", 20), "age").getValue()).isEqualTo(20);
  }

  @Test
  @DisplayName("없는 키는 canRead=false")
  void canReadMissingKey() {
    assertThat(accessor.canRead(null, Map.of("name", "bob"), "age")).isFalse();
  }

  @Test
  @DisplayName("Map이 아니면 AccessException을 던진다")
  void readNonMapThrows() {
    assertThrows(AccessException.class, () -> accessor.read(null, new Object(), "age"));
  }

  @Test
  @DisplayName("write는 UnsupportedOperationException을 던진다")
  void writeAlwaysThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> accessor.write(null, Map.of(), "key", "v"));
  }
}
