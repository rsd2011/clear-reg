package com.example.admin.maskingpolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.common.policy.MaskingMatch;
import com.example.common.policy.MaskingPolicyProvider;
import com.example.testing.bdd.Scenario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingEvaluator 테스트")
class MaskingEvaluatorTest {

  private MaskingPolicyProvider maskingPolicyProvider;
  private MaskingEvaluator evaluator;

  @BeforeEach
  void setUp() {
    maskingPolicyProvider = mock(MaskingPolicyProvider.class);
    evaluator = new MaskingEvaluator(new AuthCurrentUserProvider(), maskingPolicyProvider);
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
          "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(true)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      Scenario.given("마스킹 허용 (UNMASK >= READ)", () -> evaluator.mask("ORG_NAME", "Headquarters"))
          .then("원문 노출", value -> assertThat(value).isEqualTo("Headquarters"));
    }

    @Test
    @DisplayName("Given 마스킹 규칙 When mask 호출 Then 정책에 따른 값으로 변환한다")
    void givenMaskedContext_whenMasking_thenRedact() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

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
          "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.empty());

      Scenario.given("정책 미매칭", () -> evaluator.mask("UNKNOWN_TAG", "secret"))
          .then("기본 마스킹", value -> assertThat(value).isEqualTo("***"));
    }

    @Test
    @DisplayName("Given 화이트리스트(maskingEnabled=false) When mask 호출 Then 원본 값이 반환된다")
    void givenWhitelist_whenMasking_thenReturnRawValue() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(false)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      Scenario.given("화이트리스트", () -> evaluator.mask("PUBLIC_INFO", "public data"))
          .then("원본 반환", value -> assertThat(value).isEqualTo("public data"));
    }

    @Test
    @DisplayName("Given PHONE DataKind When mask 호출 Then 부분 마스킹된다")
    void givenPhoneDataKind_whenMasking_thenPartialMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // PHONE DataKind는 PARTIAL 규칙을 가짐
      Scenario.given("PHONE DataKind (PARTIAL 규칙)", () -> evaluator.mask("PHONE", "01012345678"))
          .then("부분 마스킹 (앞2, 뒤2 노출)", value -> assertThat(value).isEqualTo("01***78"));
    }

    @Test
    @DisplayName("Given SSN DataKind When mask 호출 Then FULL 마스킹된다")
    void givenSsnDataKind_whenMasking_thenFullMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // SSN DataKind는 FULL 규칙을 가짐
      Object result = evaluator.mask("SSN", "123456789");
      assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Given EMAIL DataKind When mask 호출 Then PARTIAL 마스킹된다")
    void givenEmailDataKind_whenMasking_thenPartialMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // EMAIL DataKind는 PARTIAL 규칙을 가짐
      Object result = evaluator.mask("EMAIL", "user@example.com");
      assertThat(result).isNotEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Given 알 수 없는 DataKind When mask 호출 Then 기본 마스킹(FULL)이 적용된다")
    void givenUnknownDataKind_whenMasking_thenUseDefaultMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // 알 수 없는 DataKind는 DEFAULT (FULL 규칙)
      Object result = evaluator.mask("UNKNOWN_DATA", "some-data");
      assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Given CARD_NO DataKind When mask 호출 Then PARTIAL 마스킹된다")
    void givenCardNoDataKind_whenMasking_thenPartialMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // CARD_NO DataKind는 PARTIAL 규칙을 가짐 (앞 2자리, 뒤 2자리 유지)
      Object result = evaluator.mask("CARD_NO", "4111111111111111");
      assertThat(result).isNotEqualTo("4111111111111111");
      assertThat(result.toString()).startsWith("41").endsWith("11");
    }

    @Test
    @DisplayName("Given 알 수 없는 DataKind When mask 호출 Then DEFAULT(FULL) 마스킹된다")
    void givenUnknownDataKindFallsbackToDefault_whenMasking_thenFullMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // 알 수 없는 DataKind(PASSWORD 등)는 DEFAULT로 폴백되어 FULL 마스킹
      Object result = evaluator.mask("PASSWORD", "secret123");
      assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Given DEFAULT DataKind When mask 호출 Then FULL 마스킹된다")
    void givenDefaultDataKind_whenMasking_thenFullMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // DEFAULT DataKind는 FULL 규칙
      Object result = evaluator.mask("DEFAULT", "plainData");
      assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Given 화이트리스트 + auditEnabled=true When mask 호출 Then 원본 반환 및 감사 로그")
    void givenWhitelistWithAudit_whenMasking_thenReturnRawAndLog() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "AUDIT_GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(false)
          .auditEnabled(true)  // 감사 로그 활성화
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // 화이트리스트 + auditEnabled=true 경로
      Object result = evaluator.mask("WHITELIST_DATA", "sensitive");
      assertThat(result).isEqualTo("sensitive");
    }
  }

  @Nested
  @DisplayName("부분 마스킹 경계 케이스")
  class PartialMaskEdgeCases {

    @Test
    @DisplayName("Given 4자 이하 문자열 When PARTIAL 마스킹 Then 기본 마스킹 적용")
    void givenShortString_whenPartialMask_thenDefaultMask() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // PHONE은 PARTIAL 규칙을 가지지만 4자 이하면 기본 마스킹
      assertThat(evaluator.mask("PHONE", "ABC")).isEqualTo("***");
      assertThat(evaluator.mask("PHONE", "ABCD")).isEqualTo("***");
    }

    @Test
    @DisplayName("Given null 값 When 마스킹 Then null 반환")
    void givenNullValue_whenMask_thenReturnNull() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      assertThat(evaluator.mask("NULLABLE", null)).isNull();
    }
  }

  @Nested
  @DisplayName("타입별 마스킹")
  class TypeBasedMasking {

    @Test
    @DisplayName("Given Boolean 값 When 마스킹 Then false 반환")
    void givenBooleanValue_whenMask_thenReturnFalse() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // SSN DataKind는 FULL 규칙
      assertThat(evaluator.mask("SSN", true)).isEqualTo(false);
      assertThat(evaluator.mask("SSN", Boolean.TRUE)).isEqualTo(false);
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
    @DisplayName("Given UNMASK 권한 When mask Then 원본 반환")
    void givenUnmaskPermission_whenMask_thenReturnRaw() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // UNMASK 권한이 있으면 원본 반환
      assertThat(evaluator.mask("DATA", "secret")).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given null actionCode in context When mask Then 기본값 READ 사용")
    void givenNullActionCodeInContext_whenMask_thenUseFallbackRead() {
      // actionCode가 null인 context 생성
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, null, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(false)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // currentAction=READ(fallback), canUnmask=false → 마스킹
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
          "user", "ORG", "AUDIT", FeatureCode.ORGANIZATION, ActionCode.UNMASK, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(true)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // auditEnabled=true 경로 확인 (로그 출력은 검증 불가, 동작만 확인)
      Object result = evaluator.mask("AUDITED", "secret");
      assertThat(result).isEqualTo("secret");
    }

    @Test
    @DisplayName("Given auditEnabled=true When 언마스킹 차단 Then 경고 로그 기록 (동작 검증)")
    void givenAuditEnabled_whenUnmaskBlocked_thenWarnLogRecorded() {
      AuthContext context = AuthContext.of(
          "user", "ORG", "DEFAULT", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      MaskingMatch match = MaskingMatch.builder()
          .policyId(UUID.randomUUID())
          .maskingEnabled(true)
          .auditEnabled(true)
          .priority(100)
          .build();
      when(maskingPolicyProvider.evaluate(any())).thenReturn(Optional.of(match));

      // auditEnabled=true + 언마스킹 차단 경로 확인
      Object result = evaluator.mask("AUDITED", "secret");
      assertThat(result).isEqualTo("***");
    }
  }
}
