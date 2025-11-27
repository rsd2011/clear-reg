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
import org.junit.jupiter.api.Nested;
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

  @Nested
  @DisplayName("기본 마스킹 시나리오")
  class BasicMaskingScenarios {

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

    @Test
    @DisplayName("Given TOKENIZE 규칙 When mask 호출 Then 해시값이 반환된다")
    void givenTokenizeMaskRule_whenMasking_thenReturnHash() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("TOKEN_DATA")
          .maskRule("TOKENIZE")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      Object result = evaluator.mask("TOKEN_DATA", "secret-token");
      assertThat(result).isInstanceOf(String.class);
      assertThat(((String) result).length()).isEqualTo(8);
    }

    @Test
    @DisplayName("Given 알 수 없는 규칙 When mask 호출 Then 기본 마스킹이 적용된다")
    void givenUnknownMaskRule_whenMasking_thenUseDefaultMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("DATA")
          .maskRule("UNKNOWN_RULE")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      Object result = evaluator.mask("DATA", "some-data");
      assertThat(result).isEqualTo("***");
    }
  }

  @Nested
  @DisplayName("부분 마스킹 경계 케이스")
  class PartialMaskEdgeCases {

    @Test
    @DisplayName("Given 4자 이하 문자열 When PARTIAL 마스킹 Then 기본 마스킹 적용")
    void givenShortString_whenPartialMask_thenDefaultMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("SHORT")
          .maskRule("PARTIAL")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("SHORT", "ABC")).isEqualTo("***");
      assertThat(evaluator.mask("SHORT", "ABCD")).isEqualTo("***");
    }

    @Test
    @DisplayName("Given null 값 When PARTIAL 마스킹 Then null 반환")
    void givenNullValue_whenPartialMask_thenReturnNull() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("NULLABLE")
          .maskRule("PARTIAL")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("NULLABLE", null)).isNull();
    }

    @Test
    @DisplayName("Given null 값 When HASH 마스킹 Then null 반환")
    void givenNullValue_whenHashMask_thenReturnNull() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("NULLABLE")
          .maskRule("HASH")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("NULLABLE", null)).isNull();
    }
  }

  @Nested
  @DisplayName("타입별 마스킹")
  class TypeBasedMasking {

    @Test
    @DisplayName("Given Boolean 값 When FULL 마스킹 Then false 반환")
    void givenBooleanValue_whenFullMask_thenReturnFalse() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("FLAG")
          .maskRule("FULL")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("FLAG", true)).isEqualTo(false);
      assertThat(evaluator.mask("FLAG", Boolean.TRUE)).isEqualTo(false);
    }

    @Test
    @DisplayName("Given null 값 When 컨텍스트 없이 mask Then null 반환")
    void givenNullValue_whenMaskWithoutContext_thenReturnNull() {
      assertThat(evaluator.mask("TAG", null)).isNull();
    }
  }

  @Nested
  @DisplayName("ActionCode 파싱")
  class ActionCodeParsing {

    @Test
    @DisplayName("Given null requiredActionCode When mask Then 기본값 UNMASK 사용")
    void givenNullRequiredActionCode_whenMask_thenUseFallback() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("DATA")
          .maskRule("FULL")
          .requiredActionCode(null)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // null이면 기본값 UNMASK 사용, 현재 action도 UNMASK이므로 원본 반환
      assertThat(evaluator.mask("DATA", "secret")).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given 빈 requiredActionCode When mask Then 기본값 UNMASK 사용")
    void givenBlankRequiredActionCode_whenMask_thenUseFallback() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("DATA")
          .maskRule("FULL")
          .requiredActionCode("   ")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("DATA", "secret")).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given 잘못된 requiredActionCode When mask Then 기본값 UNMASK 사용")
    void givenInvalidRequiredActionCode_whenMask_thenUseFallback() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("DATA")
          .maskRule("FULL")
          .requiredActionCode("INVALID_ACTION")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // INVALID_ACTION은 기본값 UNMASK로 처리, 현재도 UNMASK이므로 원본 반환
      assertThat(evaluator.mask("DATA", "secret")).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given null actionCode in context When mask Then 기본값 READ 사용")
    void givenNullActionCodeInContext_whenMask_thenUseFallbackRead() {
      // actionCode가 null인 context 생성
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, null, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("DATA")
          .maskRule("FULL")
          .requiredActionCode("UNMASK")
          .auditEnabled(false)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // currentAction=READ(fallback), required=UNMASK → READ.satisfies(UNMASK)=false → 마스킹
      assertThat(evaluator.mask("DATA", "secret")).isEqualTo("***");
    }
  }

  @Nested
  @DisplayName("감사 로그 활성화/비활성화")
  class AuditLogging {

    @Test
    @DisplayName("Given auditEnabled=true When 언마스킹 성공 Then 로그 기록 (확인 불가, 동작 검증)")
    void givenAuditEnabled_whenUnmaskGranted_thenLogRecorded() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("AUDITED")
          .maskRule("FULL")
          .requiredActionCode("READ")
          .auditEnabled(true)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // auditEnabled=true 경로 확인 (로그 출력은 검증 불가, 동작만 확인)
      Object result = evaluator.mask("AUDITED", "secret");
      assertThat(result).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given auditEnabled=true When 언마스킹 차단 Then 경고 로그 기록 (동작 검증)")
    void givenAuditEnabled_whenUnmaskBlocked_thenWarnLogRecorded() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      DataPolicyMatch match = DataPolicyMatch.builder()
          .policyId(UUID.randomUUID())
          .sensitiveTag("AUDITED")
          .maskRule("FULL")
          .requiredActionCode("UNMASK")
          .auditEnabled(true)
          .priority(100)
          .build();
      when(dataPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // auditEnabled=true + 언마스킹 차단 경로 확인
      Object result = evaluator.mask("AUDITED", "secret");
      assertThat(result).isEqualTo("***");
    }
  }
}
