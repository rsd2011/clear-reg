package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.FieldMaskRule;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.testing.bdd.Scenario;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataPolicyEvaluator 테스트")
class DataPolicyEvaluatorTest {

  private final DataPolicyEvaluator evaluator = new DataPolicyEvaluator(new AuthCurrentUserProvider());

  @AfterEach
  void tearDown() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("Given UNMASK 권한 When mask 호출 Then 원본 값이 반환된다")
  void givenUnmaskPermission_whenMasking_thenReturnRawValue() {
    FieldMaskRule rule = new FieldMaskRule("ORG_NAME", "***", ActionCode.READ, true);
    AuthContext context =
        new AuthContext(
            "user",
            "ORG",
            "AUDIT",
            FeatureCode.ORGANIZATION,
            ActionCode.UNMASK,
            RowScope.ALL,
            Map.of("ORG_NAME", rule));
    AuthContextHolder.set(context);

    Scenario.given("마스킹 허용", () -> evaluator.mask("ORG_NAME", "Headquarters"))
        .then("원문 노출", value -> assertThat(value).isEqualTo("Headquarters"));
  }

  @Test
  @DisplayName("Given 마스킹 규칙 When mask 호출 Then 정책에 따른 값으로 변환한다")
  void givenMaskedContext_whenMasking_thenRedact() {
    FieldMaskRule rule = new FieldMaskRule("SALARY", "###", ActionCode.UNMASK, false);
    AuthContext context =
        new AuthContext(
            "user",
            "ORG",
            "AUDIT",
            FeatureCode.ORGANIZATION,
            ActionCode.READ,
            RowScope.OWN,
            Map.of("SALARY", rule));
    AuthContextHolder.set(context);

    Scenario.given("마스킹 필요", () -> evaluator.mask("SALARY", 1500000))
        .then("정책에 따라 가림", value -> assertThat(value).isEqualTo(0));
  }

  @Test
  @DisplayName("Given 인증 컨텍스트 없음 When mask 호출 Then 기본 마스킹 값으로 대체한다")
  void givenNoContext_whenMasking_thenUseDefaultPlaceholder() {
    Scenario.given("컨텍스트 없음", () -> evaluator.mask("EMAIL", "user@example.com"))
        .then("기본 마스킹", value -> assertThat(value).isEqualTo("***"));
  }
}
