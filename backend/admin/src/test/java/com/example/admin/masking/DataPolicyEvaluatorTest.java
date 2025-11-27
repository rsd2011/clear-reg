package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.security.RowScope;
import com.example.testing.bdd.Scenario;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataPolicyEvaluator 테스트")
class DataPolicyEvaluatorTest {

  private DataPolicyProvider dataPolicyProvider;
  private DataPolicyEvaluator evaluator;

  @BeforeEach
  void setUp() {
    dataPolicyProvider = mock(DataPolicyProvider.class);
    evaluator = new DataPolicyEvaluator(new AuthCurrentUserProvider(), dataPolicyProvider);
  }

  @AfterEach
  void tearDown() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("Given UNMASK 권한 When mask 호출 Then 원본 값이 반환된다")
  void givenUnmaskPermission_whenMasking_thenReturnRawValue() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL);
    AuthContextHolder.set(context);

    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("ORG_NAME")
        .maskRule("FULL")
        .requiredActionCode("READ")
        .auditEnabled(true)
        .priority(100)
        .build();
    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

    Scenario.given("마스킹 허용 (UNMASK >= READ)", () -> evaluator.mask("ORG_NAME", "Headquarters"))
        .then("원문 노출", value -> assertThat(value).isEqualTo("Headquarters"));
  }

  @Test
  @DisplayName("Given 마스킹 규칙 When mask 호출 Then 정책에 따른 값으로 변환한다")
  void givenMaskedContext_whenMasking_thenRedact() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN);
    AuthContextHolder.set(context);

    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("SALARY")
        .maskRule("FULL")
        .requiredActionCode("UNMASK")
        .auditEnabled(false)
        .priority(100)
        .build();
    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

    Scenario.given("마스킹 필요 (READ < UNMASK)", () -> evaluator.mask("SALARY", 1500000))
        .then("숫자는 0으로 마스킹", value -> assertThat(value).isEqualTo(0));
  }

  @Test
  @DisplayName("Given 인증 컨텍스트 없음 When mask 호출 Then 기본 마스킹 값으로 대체한다")
  void givenNoContext_whenMasking_thenUseDefaultPlaceholder() {
    Scenario.given("컨텍스트 없음", () -> evaluator.mask("EMAIL", "user@example.com"))
        .then("기본 마스킹", value -> assertThat(value).isEqualTo("***"));
  }

  @Test
  @DisplayName("Given 매칭 정책 없음 When mask 호출 Then 기본 마스킹 값으로 대체한다")
  void givenNoPolicyMatch_whenMasking_thenUseDefaultPlaceholder() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    AuthContextHolder.set(context);

    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.empty());

    Scenario.given("정책 미매칭", () -> evaluator.mask("UNKNOWN_TAG", "secret"))
        .then("기본 마스킹", value -> assertThat(value).isEqualTo("***"));
  }

  @Test
  @DisplayName("Given NONE 규칙 When mask 호출 Then 원본 값이 반환된다")
  void givenNoneMaskRule_whenMasking_thenReturnRawValue() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    AuthContextHolder.set(context);

    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("PUBLIC_INFO")
        .maskRule("NONE")
        .requiredActionCode(null)
        .auditEnabled(false)
        .priority(100)
        .build();
    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

    Scenario.given("NONE 규칙", () -> evaluator.mask("PUBLIC_INFO", "public data"))
        .then("원본 반환", value -> assertThat(value).isEqualTo("public data"));
  }

  @Test
  @DisplayName("Given PARTIAL 규칙 When mask 호출 Then 부분 마스킹된다")
  void givenPartialMaskRule_whenMasking_thenPartialMask() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    AuthContextHolder.set(context);

    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("PHONE")
        .maskRule("PARTIAL")
        .requiredActionCode("UNMASK")
        .auditEnabled(false)
        .priority(100)
        .build();
    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

    Scenario.given("PARTIAL 규칙", () -> evaluator.mask("PHONE", "01012345678"))
        .then("부분 마스킹 (앞2, 뒤2 노출)", value -> assertThat(value).isEqualTo("01***78"));
  }

  @Test
  @DisplayName("Given HASH 규칙 When mask 호출 Then 해시값이 반환된다")
  void givenHashMaskRule_whenMasking_thenReturnHash() {
    AuthContext context = AuthContext.of(
        "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    AuthContextHolder.set(context);

    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("SSN")
        .maskRule("HASH")
        .requiredActionCode("UNMASK")
        .auditEnabled(false)
        .priority(100)
        .build();
    when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

    Object result = evaluator.mask("SSN", "123456789");
    assertThat(result).isInstanceOf(String.class);
    assertThat(((String) result).length()).isEqualTo(8); // 해시 앞 8자리
  }
}
