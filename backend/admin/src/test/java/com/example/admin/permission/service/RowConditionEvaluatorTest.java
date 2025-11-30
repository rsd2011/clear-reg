package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RowConditionEvaluator 테스트")
class RowConditionEvaluatorTest {

  RowConditionEvaluator evaluator = new RowConditionEvaluator();

  @Nested
  @DisplayName("isAllowed 메서드")
  class IsAllowedTest {

    @Test
    @DisplayName("Given null expression When isAllowed Then true 반환")
    void givenNullExpression_whenIsAllowed_thenTrue() {
      boolean result = evaluator.isAllowed(null, Map.of("team", "DEV"));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given blank expression When isAllowed Then true 반환")
    void givenBlankExpression_whenIsAllowed_thenTrue() {
      boolean result = evaluator.isAllowed("   ", Map.of("team", "DEV"));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given empty expression When isAllowed Then true 반환")
    void givenEmptyExpression_whenIsAllowed_thenTrue() {
      boolean result = evaluator.isAllowed("", Map.of("team", "DEV"));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 속성이 일치하면 When isAllowed Then true 반환")
    void givenMatchingAttribute_whenIsAllowed_thenTrue() {
      boolean result = evaluator.isAllowed("team == 'OPS'", Map.of("team", "OPS"));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 속성이 일치하지 않으면 When isAllowed Then false 반환")
    void returnsFalseWhenAttributeMissing() {
      boolean allowed = evaluator.isAllowed("team == 'OPS'", Map.of("team", "DEV"));
      assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Given 복잡한 조건식 When isAllowed Then 올바르게 평가")
    void givenComplexExpression_whenIsAllowed_thenEvaluatesCorrectly() {
      boolean result = evaluator.isAllowed(
          "team == 'DEV' and level >= 3",
          Map.of("team", "DEV", "level", 5));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 맵에 없는 속성 참조 When isAllowed Then 예외 발생")
    void givenMissingKey_whenIsAllowed_thenThrows() {
      assertThatThrownBy(() -> evaluator.isAllowed("missing == 'value'", Map.of("other", "value")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("잘못된 RowScope 조건 표현식입니다");
    }

    @Test
    @DisplayName("Given 잘못된 표현식 When isAllowed Then 예외 발생")
    void givenInvalidExpression_whenIsAllowed_thenThrows() {
      assertThatThrownBy(() -> evaluator.isAllowed("invalid syntax !!!", Map.of("team", "DEV")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("잘못된 RowScope 조건 표현식입니다");
    }

    @Test
    @DisplayName("Given empty map When isAllowed Then 예외 발생 (속성 참조 시)")
    void givenEmptyMap_whenIsAllowedWithAttribute_thenThrows() {
      assertThatThrownBy(() -> evaluator.isAllowed("team == 'DEV'", Collections.emptyMap()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("잘못된 RowScope 조건 표현식입니다");
    }

    @Test
    @DisplayName("Given or 조건식 When isAllowed Then 올바르게 평가")
    void givenOrExpression_whenIsAllowed_thenEvaluatesCorrectly() {
      boolean result = evaluator.isAllowed(
          "team == 'DEV' or team == 'OPS'",
          Map.of("team", "OPS"));
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 숫자 비교 조건 When isAllowed Then 올바르게 평가")
    void givenNumericComparison_whenIsAllowed_thenEvaluatesCorrectly() {
      boolean result = evaluator.isAllowed("level > 2", Map.of("level", 3));
      assertThat(result).isTrue();

      boolean resultFalse = evaluator.isAllowed("level > 5", Map.of("level", 3));
      assertThat(resultFalse).isFalse();
    }
  }

  @Nested
  @DisplayName("validate 메서드")
  class ValidateTest {

    @Test
    @DisplayName("Given null expression When validate Then 아무 일도 일어나지 않음")
    void givenNullExpression_whenValidate_thenNoException() {
      evaluator.validate(null);
      // No exception means success
    }

    @Test
    @DisplayName("Given blank expression When validate Then 아무 일도 일어나지 않음")
    void givenBlankExpression_whenValidate_thenNoException() {
      evaluator.validate("   ");
      // No exception means success
    }

    @Test
    @DisplayName("Given empty expression When validate Then 아무 일도 일어나지 않음")
    void givenEmptyExpression_whenValidate_thenNoException() {
      evaluator.validate("");
      // No exception means success
    }

    @Test
    @DisplayName("Given 유효한 expression When validate Then 아무 일도 일어나지 않음")
    void givenValidExpression_whenValidate_thenNoException() {
      evaluator.validate("team == 'DEV' and level >= 3");
      // No exception means success
    }

    @Test
    @DisplayName("Given 잘못된 expression When validate Then 예외 발생")
    void givenInvalidExpression_whenValidate_thenThrows() {
      assertThatThrownBy(() -> evaluator.validate("invalid syntax !!!"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("잘못된 RowScope 조건 표현식입니다");
    }
  }

  @Nested
  @DisplayName("캐싱 동작")
  class CachingTest {

    @Test
    @DisplayName("Given 동일한 expression When 여러 번 호출 Then 캐시 사용")
    void givenSameExpression_whenCalledMultipleTimes_thenUsesCached() {
      String expression = "team == 'DEV'";

      // 첫 호출 - 파싱하고 캐시
      evaluator.isAllowed(expression, Map.of("team", "DEV"));

      // 두 번째 호출 - 캐시된 파싱 결과 사용
      boolean result = evaluator.isAllowed(expression, Map.of("team", "OPS"));
      assertThat(result).isFalse();

      // validate에서도 동일한 캐시 사용
      evaluator.validate(expression);
    }
  }
}
